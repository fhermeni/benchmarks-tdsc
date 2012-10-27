/*
 * Copyright (c) Fabien Hermenier
 *
 * This file is part of Entropy.
 *
 * Entropy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Entropy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */
package microDSN;

import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.action.Migration;
import entropy.plan.action.Run;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.*;

import java.util.*;

/**
 * A CustomizablePlannerModule based on Choco.
 *
 * @author Fabien Hermenier
 */
public class FFDStyleVMAndHostAffinityAmongRP extends CustomizablePlannerModule {

    private boolean repair = true;

    /**
     * Make a new plan module.
     *
     * @param eval to evaluate the duration of the actions.
     */
    public FFDStyleVMAndHostAffinityAmongRP(DurationEvaluator eval) {
        super(eval);
    }

    @Override
    public List<SolutionStatistics> getSolutionsStatistics() {
        return null;
    }

    /**
     * @return some statistics about the solving process
     */
    @Override
    public SolvingStatistics getSolvingStatistics() {
        return null;
    }

    Map<VirtualMachine,ManagedElementSet<VirtualMachine>> antiAffinity;

    Map<VirtualMachine,ManagedElementSet<VirtualMachine>> affinity;

    Map<VirtualMachine, ManagedElementSet<Node>> whiteList;

    private Map<VirtualMachine,Among> relatedAmong;

    @Override
    public TimedReconfigurationPlan compute(Configuration src,
                                            ManagedElementSet<VirtualMachine> run,
                                            ManagedElementSet<VirtualMachine> wait,
                                            ManagedElementSet<VirtualMachine> sleep,
                                            ManagedElementSet<VirtualMachine> stop,
                                            ManagedElementSet<Node> on,
                                            ManagedElementSet<Node> off,
                                            List<VJob> q) throws PlanException {

        List<PlacementConstraint> cstrs = new ArrayList<PlacementConstraint>();
        ManagedElementSet<VirtualMachine> toRepair;

        HashSet<String> unsupported  = new HashSet<String>();


        if (repair) {
            toRepair = src.getWaitings().clone();
            for (Node n : Configurations.futureOverloadedNodes(src)) {
                toRepair.addAll(src.getRunnings(n));
            }
        } else {
            toRepair = run.clone();
        }

        antiAffinity = new HashMap<VirtualMachine, ManagedElementSet<VirtualMachine>>();
        affinity = new HashMap<VirtualMachine, ManagedElementSet<VirtualMachine>>();
        whiteList = new HashMap<VirtualMachine, ManagedElementSet<Node>>();
        relatedAmong = new HashMap<VirtualMachine, Among>();
        ManagedElementSet<VirtualMachine> rooted = new SimpleManagedElementSet<VirtualMachine>();
        ManagedElementSet<Node> free = new SimpleManagedElementSet<Node>();
        for (VJob v : q) {
            for (PlacementConstraint cstr : v.getConstraints()) {
                if (repair) {
                    toRepair.addAll(cstr.getMisPlaced(src));
                }
                cstrs.add(cstr);
                if (cstr instanceof Spread) {
                    for (VirtualMachine vm : cstr.getAllVirtualMachines()) {
                        if (antiAffinity.get(vm) == null) {
                            antiAffinity.put(vm, cstr.getAllVirtualMachines());
                        } else {
                            antiAffinity.get(vm).addAll(cstr.getAllVirtualMachines());
                        }
                    }
                } else if (cstr instanceof Gather) {
                    for (VirtualMachine vm : cstr.getAllVirtualMachines()) {
                        if (affinity.get(vm) == null) {
                            affinity.put(vm, cstr.getAllVirtualMachines());
                        } else {
                            affinity.get(vm).addAll(cstr.getAllVirtualMachines());
                        }
                    }
                } else if (cstr instanceof Root) {
                    rooted.addAll(cstr.getAllVirtualMachines());
                } else if (cstr instanceof Fence) {
                    for (VirtualMachine vm : cstr.getAllVirtualMachines()) {
                        if (whiteList.get(vm) == null) {
                            whiteList.put(vm, cstr.getNodes());
                        } else {
                            whiteList.get(vm).retainAll(cstr.getNodes()); //Intersection with this set and the current
                        }
                    }
                } else if (cstr instanceof Ban) {
                    for (VirtualMachine vm : cstr.getAllVirtualMachines()) {
                        if (whiteList.get(vm) == null) {
                            whiteList.put(vm, src.getAllNodes());

                        }
                        whiteList.get(vm).removeAll(cstr.getNodes());
                    }
                } else if (cstr instanceof Among) {
                    Among a = (Among) cstr;
                    for (VirtualMachine vm : a.getAllVirtualMachines()) {
                        relatedAmong.put(vm, a);
                        if (whiteList.get(vm) != null) {
                            whiteList.get(vm).retainAll(a.getNodes());
                        }
                    }
                }

                else {
                    unsupported.add(cstr.getClass().getSimpleName());
                }
            }
        }

        Configuration dst = new SimpleConfiguration();
        for (Node n : src.getOnlines()) {
            dst.addOnline(n);
        }
        for (Node n : src.getOfflines()) {
            dst.addOffline(n);
        }


        //Place the VMs that are supposed to be fine
        if (repair) {
            for (Node n : src.getOnlines()) {
                for (VirtualMachine vm : src.getRunnings(n)) {
                    if (!toRepair.contains(vm)) {
                        dst.setRunOn(vm, n);
                        notifyAmong(vm, n);
                    }
                }
            }
        }

        //Biggest VMs first
        TimedReconfigurationPlan plan = new DefaultTimedReconfigurationPlan(src);
        VirtualMachineComparator vmCmp = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);
        Collections.sort(toRepair, vmCmp);
        for (VirtualMachine vm : toRepair) {

            //Check for the current node
            Node current = src.getLocation(vm);
            if (current != null && hasEnoughFreeResources(dst, current, vm) && !disallowBySpread(dst, current, vm) && !disallowByGather(dst, current, vm) && !disallowByWhiteList(dst, current, vm)) {
                dst.setRunOn(vm, current);
            } else {
                if (rooted.contains(vm)) {
                    throw new PlanException("Unable to let the rooted VM " + vm.getName() + " on " + src.getLocation(vm).getName());
                }
                for (Node n : dst.getOnlines()) {
                    if (n != src.getLocation(vm)) {
                        if (hasEnoughFreeResources(dst, n, vm) && !disallowBySpread(dst, n, vm) && !disallowByGather(dst, n, vm) && !disallowByWhiteList(dst, n, vm)) {
                            dst.setRunOn(vm, n);
                            notifyAmong(vm, n);
                            if (src.getLocation(vm) != n) {
                                if (src.getLocation(vm) != null) {
                                    free.add(src.getLocation(vm));
                                    plan.add(new Migration(vm, src.getLocation(vm), n, 0, 5));
                                    notifyMoved(vm, src.getLocation(vm));
                                } else {
                                    plan.add(new Run(vm, n, 0, 5));
                                }
                            }
                            break;
                        }
                    }
                }
            }

        }
        int nbViolations = 0;
        HashSet<String> violated = new HashSet<String>();
        for (PlacementConstraint cstr : cstrs) {
            if (!cstr.isSatisfied(dst)) {
                nbViolations++;
                violated.add(cstr.getClass().getSimpleName());
            }
        }
        if (nbViolations > 0) {
            logger.info(getClass().getSimpleName() + ": " + nbViolations + "/" + cstrs.size() + " violation(s): " + violated);
        }
        assert (Configurations.isFutureViable(dst));
        return plan;
    }

    private void notifyMoved(VirtualMachine vm, Node location) {
        if (!moved.containsKey(location)) {
            moved.put(location, new SimpleManagedElementSet<VirtualMachine>(vm));
        } else {
            moved.get(location).add(vm);
        }
    }

    private void notifyAmong(VirtualMachine vm, Node selected) throws PlanException {
        //The VM is in an among constraint
        if (relatedAmong.containsKey(vm)) {
            //Get the choosed group
            Among a = relatedAmong.get(vm);

            for (ManagedElementSet<Node> group : a.getGroups()) {
                if (group.contains(selected)) {
                    for (VirtualMachine relatedVM : a.getAllVirtualMachines()) {
                        if (whiteList.containsKey(relatedVM)) {
                            whiteList.get(relatedVM).retainAll(group);
                            if (whiteList.get(relatedVM).isEmpty()) {
                                throw new PlanException("Contradiction with Among");
                            }
                        } else {
                            whiteList.put(relatedVM, group.clone());
                        }
                    }
                    return;
                }
            }
            throw new PlanException("Zarb");
        }
    }


    private Map<Node,ManagedElementSet<VirtualMachine>> moved = new HashMap<Node, ManagedElementSet<VirtualMachine>>();

    public boolean hasEnoughFreeResources(Configuration dst, Node n, VirtualMachine vm) {
        int [] used = ManagedElementSets.sum(dst.getRunnings(n), ResourcePicker.VMRc.cpuDemand, ResourcePicker.VMRc.memoryDemand);
        if (moved.containsKey(n)) {
            int [] overhead = ManagedElementSets.sum(moved.get(n), ResourcePicker.VMRc.cpuConsumption, ResourcePicker.VMRc.memoryConsumption);
            used[0] += overhead[0];
            used[1] += overhead[1];
        }
        return (n.getCPUCapacity() >= used[0] + vm.getCPUDemand() && n.getMemoryCapacity() >= used[1] + vm.getMemoryDemand());
    }

    /**
     * Use the repair mode.
     *
     * @param b {@code true} to use the repair mode
     */
    public void setRepairMode(boolean b) {
        repair = b;
    }

    public boolean disallowBySpread(Configuration dst, Node n, VirtualMachine vm) {
        ManagedElementSet<VirtualMachine> ennemies = antiAffinity.get(vm);
        if (ennemies != null) {
            for (VirtualMachine v : ennemies) {
                Node on = dst.getLocation(v);
                if (on != null && on == n) { //The node is already used by a VM connected to this one using a Spread constraint
                    return true;
                }
            }
        }
        return false;
    }

    public boolean disallowByGather(Configuration dst, Node n, VirtualMachine vm) {
        ManagedElementSet<VirtualMachine> friends = affinity.get(vm);
        if (friends != null) {
            for (VirtualMachine v : friends) {
                Node on = dst.getLocation(v);
                if (on != null && on != n) { //The node is not used by a friend VM
                    return true;
                }
            }
        }
        return false;
    }

    public boolean disallowByWhiteList(Configuration dst, Node n, VirtualMachine vm) {
        ManagedElementSet<Node> allowed = whiteList.get(vm);
        if (allowed != null) {
            return !allowed.contains(n);
        }
        return false;
    }
}
