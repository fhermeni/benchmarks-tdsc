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

import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valselector.MinVal;
import choco.cp.solver.search.integer.varselector.StaticVarOrder;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;
import entropy.configuration.*;
import entropy.plan.*;
import entropy.plan.choco.DefaultReconfigurationProblem;
import entropy.plan.choco.ReconfigurationProblem;
import entropy.plan.choco.actionModel.*;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSliceHeights;
import entropy.plan.choco.constraint.pack.SatisfyDemandingSlicesHeightsFastBP;
import entropy.plan.choco.constraint.sliceScheduling.SlicesPlanner;
import entropy.plan.choco.search.HosterVarSelector;
import entropy.plan.choco.search.PureIncomingFirst;
import entropy.plan.choco.search.StayFirstSelector3;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import gnu.trove.TIntHashSet;
import gnu.trove.TLongIntHashMap;

import java.util.*;

/**
 * A Module that is only capable to launch vjobs
 *
 * @author Fabien Hermenier
 */
public class VJobLauncher extends CustomizablePlannerModule {

    /**
     * The model.
     */
    private ReconfigurationProblem model;

    private boolean repair = true;

    private List<VJob> queue;

    /**
     * Make a new plan module.
     *
     * @param eval to evaluate the duration of the actions.
     */
    public VJobLauncher(DurationEvaluator eval) {
        super(eval);
    }

    /**
     * Get the model.
     *
     * @return the model to express constraints.
     */
    public ReconfigurationProblem getModel() {
        return model;
    }

    @Override
    public List<SolutionStatistics> getSolutionsStatistics() {
        if (model == null) {
            return new ArrayList<SolutionStatistics>();
        }
        return model.getSolutionsStatistics();
    }

    /**
     * @return some statistics about the solving process
     */
    @Override
    public SolvingStatistics getSolvingStatistics() {
        if (model == null) {
            return SolvingStatistics.getStatisticsForNotSolvingProcess();
        }
        return model.getSolvingStatistics();
    }

    /**
     * the class to instantiate to generate the global constraint.
     * Default is SatisfyDemandingSlicesHeightsSimpleBP.
     */
    private SatisfyDemandingSliceHeights packingConstraintClass = new SatisfyDemandingSlicesHeightsFastBP();

    /**
     * @return the globalConstraintClass
     */
    public SatisfyDemandingSliceHeights getPackingConstraintClass() {
        return packingConstraintClass;
    }

    /**
     * @param c the globalConstraintClass to set
     */
    public void setPackingConstraintClass(SatisfyDemandingSliceHeights c) {
        packingConstraintClass = c;
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

        long st = System.currentTimeMillis();
        queue = q;

        model = null;

        ManagedElementSet<VirtualMachine> vms;
        if (repair) {
            //Look for the VMs to consider
            vms = new SimpleManagedElementSet<VirtualMachine>();
            for (VJob v : queue) {
                for (PlacementConstraint c : v.getConstraints()) {
                    if (!c.isSatisfied(src)) {
                        vms.addAll(c.getMisPlaced(src));
                    }
                }
            }
            vms.addAll(src.getRunnings(Configurations.futureOverloadedNodes(src)));
        } else {
            vms = src.getAllVirtualMachines();
        }

        model = new DefaultReconfigurationProblem(src, run, wait, sleep, stop, vms,
                on, off, getDurationEvaluator());

        Map<Class<?>, Integer> occurences = new HashMap<Class<?>, Integer>();
        int nbConstraints = 0;

        //We first translate and inject absolute constraints as they directly restrict the placement.
        //So the domain of the VMs will be already reduced for the relative constraints
        List<PlacementConstraint> relatives = new ArrayList<PlacementConstraint>();
        for (VJob vjob : queue) {
            for (PlacementConstraint c : vjob.getConstraints()) {
                try {
                    if (c.getType() == PlacementConstraint.Type.absolute) {
                        c.inject(model);
                    } else {
                        relatives.add(c);
                    }

                    if (!occurences.containsKey(c.getClass())) {
                        occurences.put(c.getClass(), 0);
                    }
                    nbConstraints++;
                    occurences.put(c.getClass(), 1 + occurences.get(c.getClass()));
                } catch (Exception e) {
                    Plan.logger.error(e.getMessage(), e);
                }
            }
        }

        for (PlacementConstraint c : relatives) {
            c.inject(model);
        }

        packingConstraintClass.add(model);
        new SlicesPlanner().add(model);

        /*
           * A pretty print of the problem
           */
        //The elements
        Plan.logger.debug(run.size() + wait.size() + sleep.size() + stop.size() + " VMs: " +
                run.size() + " will run; " + wait.size() + " will wait; " + sleep.size() + " will sleep; " + stop.size() + " will be stopped");
        Plan.logger.debug(on.size() + off.size() + " nodes: " + on.size() + " to run; " + off.size() + " to halt");
        Plan.logger.debug("Manage " + vms.size() + " VMs (" + (repair ? "repair" : "rebuild") + ")");
        if (getTimeLimit() > 0) {
            Plan.logger.debug("Timeout is " + getTimeLimit() + " seconds");
        } else {
            Plan.logger.debug("No timeout!");
        }

        //The constraints
        StringBuilder b = new StringBuilder();
        if (nbConstraints > 0) {
            b.append(nbConstraints).append(" constraints: ");
            for (Map.Entry<Class<?>, Integer> e : occurences.entrySet()) {
                b.append(e.getValue()).append(" ").append(e.getKey().getSimpleName()).append("; ");
            }

        } else {
            b.append("No constraints");
        }
        Plan.logger.debug(b.toString());
        /**
         * globalCost is equals to the sum of each action costs.
         */


        if (getTimeLimit() > 0) {
            model.setTimeLimit(getTimeLimit() * 1000);
        }

        VirtualMachineComparator dsc = new VirtualMachineComparator(false, ResourcePicker.VMRc.memoryConsumption);

        TLongIntHashMap oldLocation = new TLongIntHashMap(model.getFutureRunnings().size());

        for (VirtualMachine vm : model.getFutureRunnings()) {
            int idx = model.getVirtualMachine(vm);
            VirtualMachineActionModel a = model.getAssociatedVirtualMachineAction(idx);
            if (a.getClass() == MigratableActionModel.class || a.getClass() == ResumeActionModel.class) {
                oldLocation.put(a.getDemandingSlice().hoster().getIndex(), model.getCurrentLocation(idx));
            }
        }

        TIntHashSet[] favorites = new TIntHashSet[2];
        favorites[0] = new TIntHashSet();
        favorites[1] = new TIntHashSet();

        //Composed with nodes that do not host misplaced VMs.
        ManagedElementSet<Node> involded = src.getAllNodes().clone();
        for (Node n : involded) {
            favorites[0].add(model.getNode(n));
        }
        //System.err.println(involded.size() + " (" + favorites[0].size() + ") idylic nodes over " + src.getAllNodes().size() + " (" + favorites[1].size() + ")");
        for (VJob v : queue) {
            for (PlacementConstraint c : v.getConstraints()) {
                ManagedElementSet<VirtualMachine> myVMs = c.getAllVirtualMachines().clone();
                Collections.sort(myVMs, dsc);
                HosterVarSelector hostSelector = new HosterVarSelector(model, ActionModels.extractDemandingSlices(model.getAssociatedActions(myVMs)));
                model.addGoal(new AssignVar(hostSelector, new StayFirstSelector3(model, oldLocation, getPackingConstraintClass(), favorites, StayFirstSelector3.Option.wfMem)));
            }
        }

        ///SCHEDULING PROBLEM
        List<ActionModel> actions = new ArrayList<ActionModel>();
        for (VirtualMachineActionModel vma : model.getVirtualMachineActions()) {
            actions.add(vma);
        }
        model.addGoal(new AssignVar(new PureIncomingFirst(model, actions, new ArrayList<SConstraint>()), new MinVal()));

        model.addGoal(new AssignVar(new StaticVarOrder(model, new IntDomainVar[]{model.getEnd(), model.getEnd()}), new MinVal()));
        /*
        model.setDoMaximize(false);
        model.setRestart(false);
        model.setFirstSolution(true);
        model.generateSearchStrategy();
        ISolutionPool sp = SolutionPoolFactory.makeInfiniteSolutionPool(model.getSearchStrategy());
        model.getSearchStrategy().setSolutionPool(sp);
        */
        long ed = System.currentTimeMillis();
        generationTime = ed - st;
        logger.debug(generationTime + "ms to build the solver " + model.getNbIntConstraints() + " cstr " + model.getNbIntVars() + "+" + model.getNbBooleanVars() + " variables " + model.getNbConstants() + " cte");


        Boolean ret = model.solve();
        if (ret == null) {
            throw new PlanException("Unable to check wether a solution exists or not");
        } else {
            Plan.logger.debug("#nodes= " + model.getNodeCount() +
                    ", #backtracks= " + model.getBackTrackCount() +
                    ", #duration= " + model.getTimeCount() +
                    ", #nbsol= " + model.getNbSolutions());

            if (Boolean.FALSE.equals(ret)) {
                throw new PlanException("No solution");
            } else {
                TimedReconfigurationPlan plan = model.extractSolution();
                Configuration res = plan.getDestination();
                if (!Configurations.futureOverloadedNodes(res).isEmpty()) {
                    throw new PlanException("Resulting configuration is not viable: Overloaded nodes=" + Configurations.futureOverloadedNodes(res));
                }

                if (model.getEnd().getVal() != plan.getDuration()) {
                    throw new PlanException("Practical duration(" + plan.getDuration() + ") and theoretical (" + model.getEnd().getVal() + ") missmatch:\n" + plan);
                }
                for (VJob vjob : queue) {
                    for (PlacementConstraint c : vjob.getConstraints()) {
                        if (!c.isSatisfied(res)) {
                            throw new PlanException("Resulting configuration does not satisfy '" + c.toString() + "'");
                        }
                    }
                }
                return plan;
            }
        }
    }

    /**
     * Get all the vjobs managed by the module
     *
     * @return a list of vjobs, may be empty
     */
    public List<VJob> getQueue() {
        return queue;
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
