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

import choco.Choco;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.constraints.integer.IntExp;
import choco.kernel.solver.search.ISolutionPool;
import choco.kernel.solver.search.SolutionPoolFactory;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.action.Migration;
import entropy.plan.action.Run;
import entropy.plan.choco.BasicPlacementHeuristic2;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.ActionModel;
import entropy.plan.choco.actionModel.ActionModels;
import entropy.plan.choco.actionModel.VirtualMachineActionModel;
import entropy.plan.choco.actionModel.slice.Slice;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsSimpleBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.durationEvaluator.DurationEvaluationException;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.*;

import java.util.*;

/**
 * A CustomizablePlannerModule based on Choco.
 *
 * @author Fabien Hermenier
 */
public class FFDStyleRP extends CustomizablePlannerModule {

    private boolean repair = true;

    /**
     * Make a new plan module.
     *
     * @param eval to evaluate the duration of the actions.
     */
    public FFDStyleRP(DurationEvaluator eval) {
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

        ManagedElementSet<VirtualMachine> rooted = new SimpleManagedElementSet<VirtualMachine>();

        if (repair) {
            toRepair = new SimpleManagedElementSet<VirtualMachine>();
            for (Node n : Configurations.futureOverloadedNodes(src)) {
                toRepair.addAll(src.getRunnings(n));
            }
        } else {
            toRepair = run.clone();

        }
        for (VirtualMachine vm: run) {
            if (src.getLocation(vm) == null) {
                toRepair.add(vm);
            }
        }
        for (VJob v : q) {
            for (PlacementConstraint cstr : v.getConstraints()) {
                if (repair) {
                    toRepair.addAll(cstr.getMisPlaced(src));
                }
                cstrs.add(cstr);
                if (cstr instanceof Root) {
                    rooted.addAll(cstr.getAllVirtualMachines());
                }
                else {
                    unsupported.add(cstr.getClass().getSimpleName());
                }
            }
        }

        //logger.debug(getClass().getSimpleName() + ": " +"Unsupported constraints: " + unsupported);


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
                    }
                }
            }
        }

        //Biggest VMs first
        TimedReconfigurationPlan plan = new DefaultTimedReconfigurationPlan(src);
        VirtualMachineComparator vmCmp = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);
        Collections.sort(toRepair, vmCmp);
        //logger.debug("Manage " + toRepair.size() + " VM(s)");
        for (VirtualMachine vm : toRepair) {

            //Check for the current node
            Node current = src.getLocation(vm);
            if (current != null && hasEnoughFreeResources(dst, current, vm)) {
                dst.setRunOn(vm, current);
            } else {
                if (rooted.contains(vm) && !src.isWaiting(vm)) {
                    throw new PlanException("Unable to let the rooted VM " + vm.getName() + " on " + src.getLocation(vm).getName());
                }
                boolean placed = false;
                for (Node n : dst.getOnlines()) {
                    if (n != src.getLocation(vm)) {
                        if (hasEnoughFreeResources(dst, n, vm)) {
                            placed = true;
                            dst.setRunOn(vm, n);
                            if (src.getLocation(vm) != n) {
                                if (src.getLocation(vm) != null) {
                                    plan.add(new Migration(vm, src.getLocation(vm), n, 0, 5));
                                } else {
                                    plan.add(new Run(vm, n, 0, 5));
                                }
                            }
                            break;
                        }
                    }
                }
                if (!placed) {
                    throw new PlanException("Unable to compute a placement for VM " + vm.getName());
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

    public boolean hasEnoughFreeResources(Configuration dst, Node n, VirtualMachine vm) {
        int [] used = ManagedElementSets.sum(dst.getRunnings(n), ResourcePicker.VMRc.cpuDemand, ResourcePicker.VMRc.memoryDemand);
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
}
