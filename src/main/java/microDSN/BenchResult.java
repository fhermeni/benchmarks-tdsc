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
import entropy.plan.SolutionStatistics;
import entropy.plan.SolvingStatistics;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.action.Action;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * The result of a bench.
 * The semantic of the result depends wether the solver was able to compute at least a solution or not.
 * If it does, numbers are related to the moment the solver computes its last solution. Otherwise, numbers
 * are related to the moment it hits the timeout.
 *
 * @author Fabien Hermenier
 */
public class BenchResult {


    /**
     * The identifier of the instance.
     */
    public String id;

    public SolvingStatistics stat;

    /**
     * All the computed solutions. The last denotes the solution that create the plan and
     * the destination configuration
     */
    public TreeSet<SolutionStatistics> solutions;

    /**
     * The resulting plan.
     */
    public TimedReconfigurationPlan plan;

    /**
     * The destination configuration.
     */
    public Configuration dst;

    /**
     * The time to generate the CSP frim its description, in milliseconds.
     */
    public long generationTime;

    /**
     * A comparator to sort the solution in ascending order wrt. the objective
     */
    static Comparator<SolutionStatistics> comp = new Comparator<SolutionStatistics>() {

        @Override
        public int compare(SolutionStatistics o1, SolutionStatistics o2) {
            return o1.getTimeCount() - o2.getTimeCount();
        }
    };

    /**
     * Make a new benchresult.
     */
    public BenchResult() {
        solutions = new TreeSet<SolutionStatistics>(comp);
    }

    /**
     * Output for the screen.
     *
     * @return a String
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (id != null) {
            buffer.append(id).append(' ');
        }
        buffer.append("solved=").append(!solutions.isEmpty());
        buffer.append(", generationTime=").append(generationTime);
        buffer.append(", ").append(stat == null ? " no statistics" : stat.toString()).append("\n");
        int nb = 1;
        for (SolutionStatistics s : solutions) {
            buffer.append(' ').append(nb++).append(") ").append(s.toString()).append("\n");
        }
        if (plan != null) {
            //Metrics for the actions: number of migration, instantiate and run (run for boot, so all the run - instantiate)
            int nbMig = 0;
            int nbInstantiate = 0;
            int nbStarts = 0;

            for (Action a : plan.getActions()) {
                if (a.toString().contains("migra")) {
                    nbMig++;
                } else if (a.toString().contains("instantia")) {
                    nbInstantiate++;
                } else if (a.toString().contains("run")) {
                    nbStarts++;
                }
            }
            buffer.append(' ').append(plan.getActions().size()).append(" action(s); ").append(plan.getDuration()).append(" sec. to apply\n");
            buffer.append('\t').append(nbMig).append(" migration(s)\n");
            buffer.append('\t').append(nbInstantiate).append(" re-instantiation(s)\n");
            buffer.append('\t').append(nbStarts - nbInstantiate).append(" start(s)\n");
        }
        return buffer.toString();
    }

    /**
     * Print all the solutions for a bench
     *
     * @return a formatted String
     */
    public String toRaw() {
        StringBuilder buf = new StringBuilder();
        for (Iterator<SolutionStatistics> ite = solutions.iterator(); ite.hasNext(); ) {
            SolutionStatistics sol = ite.next();
            buf.append(id).append(' ').append(generationTime).append(' ').append(sol.toRawData());

            if (ite.hasNext()) {
                buf.append(" - - - - -\n"); //- -  as we don't have the number of actions & the application of the plan
            } else {
                //Metrics for the actions: number of migration, instantiate and run (run for boot, so all the run - instantiate)
                int nbMig = 0;
                int nbInstantiate = 0;
                int nbStarts = 0;
                for (Action a : plan.getActions()) {
                    if (a.toString().contains("migra")) {
                        nbMig++;
                    } else if (a.toString().contains("instantia")) {
                        nbInstantiate++;
                    } else if (a.toString().contains("run")) {
                        nbStarts++;
                    }
                }
                nbStarts -= nbInstantiate;
                buf.append(' ').append(plan.getActions().size()).append(' ').append(nbMig).append(' ').append(nbInstantiate).append(' ').append(nbStarts).append(' ').append(plan.getDuration());
            }
        }
        if (solutions.isEmpty()) {
            buf.append(id).append(' ').append(generationTime).append(" - - - - - - - - -");
        }
        return buf.toString();

    }
}
