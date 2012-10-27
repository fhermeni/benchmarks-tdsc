/*
 * Copyright (c) 2010 Ecole des Mines de Nantes.
 *
 *      This file is part of Entropy.
 *
 *      Entropy is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Lesser General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      Entropy is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public License
 *      along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package microDSN;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.plan.PlanException;
import entropy.plan.SolutionStatistics;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.CustomizableSplitablePlannerModule;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.vjob.VJob;

import java.io.File;
import java.util.List;

/**
 * A Bench suite to compare the solve of problems with the same initial configuration
 * but different constraints.
 *
 * @author Fabien Hermenier
 */
public class ReconfigurationProblemBench {

    /**
     * The duration evaluator to estimate the duration of actions.
     */
    private DurationEvaluator durEval = null;

    /**
     * The maximum duration of the solving process.
     */
    private int timeout;

    /**
     * The path where vjobs are stored (single file or folder).
     */
    private File vjobsPath;

    private boolean useRepair;

    private CustomizableSplitablePlannerModule.PartitioningMode pMode;

    private boolean optimize = true;

    /**
     * Make a new benchmark.
     *
     * @param eval to evaluate the duration of actions
     * @param t    the maximum duration of the solving process in seconds
     */
    public ReconfigurationProblemBench(DurationEvaluator eval, int t, boolean useRepair, CustomizableSplitablePlannerModule.PartitioningMode pMode) {
        this.durEval = eval;
        this.timeout = t;
        this.pMode = pMode;
        this.useRepair = useRepair;
    }

    public void setTimeout(int t) {
        this.timeout = t;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setPartitionMode(CustomizableSplitablePlannerModule.PartitioningMode pMode) {
        this.pMode = pMode;
    }

    public CustomizableSplitablePlannerModule.PartitioningMode getPartitionMode() {
        return this.pMode;
    }

    public void useRepair(boolean b) {
        this.useRepair = b;
    }

    public boolean useRepair() {
        return useRepair;
    }

    public void doOptimize(boolean b) {
        this.optimize = b;
    }

    public boolean doOptimize() {
        return this.optimize;
    }

    public BenchResult bench(Configuration src, Configuration dst, List<VJob> vjobs) {
        //System.err.println(src.getAllVirtualMachines().size() + " " + dst.getAllVirtualMachines().size());
        ManagedElementSet<VirtualMachine> toStop = src.getAllVirtualMachines().clone();
        toStop.removeAll(dst.getAllVirtualMachines());
        CustomizableSplitablePlannerModule rp = new CustomizableSplitablePlannerModule(durEval);
        rp.doOptimize(optimize);
        rp.setRepairMode(useRepair);
        rp.setTimeLimit(timeout);
        rp.setPartitioningMode(pMode);
        BenchResult res = new BenchResult();
        try {
            TimedReconfigurationPlan plan = rp.compute(src,
                    dst.getRunnings(),
                    dst.getWaitings(),
                    dst.getSleepings(),
                    toStop,
                    dst.getOnlines(),
                    dst.getOfflines(),
                    vjobs);
            List<SolutionStatistics> sols = rp.getSolutionsStatistics();
            res.plan = plan;
            res.dst = plan.getDestination();
            res.generationTime = rp.getGenerationTime();
            for (SolutionStatistics sol : sols) {
                res.solutions.add(sol);
            }

        } catch (PlanException e) {
            e.printStackTrace();
            //  res.solutions.add(rp.getSolvingStatistics());
        } finally {
            res.stat = rp.getSolvingStatistics();
        }
        return res;
    }

}