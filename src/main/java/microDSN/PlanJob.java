/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package microDSN;


import entropy.jobsManager.Job;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class PlanJob {


    public static final String CONFIG_KEY_SRC = "cfg-src";

    public static final String CONFIG_KEY_DST = "cfg-dst";

    public static final String VJOBS_PATH = "v";

    public static final String PROPS = "properties";

    public static final String PLAN = "plan";

    private static final String METRICS = "metrics";

    private static final String NBVJOBS = "nbVjobs";

    private PlanJob() {
    }

    public static void setSrcConfigPath(Job j, String p) {
        j.put(CONFIG_KEY_SRC, p);
    }

    public static String getSrcConfigPath(Job j) {
        return j.get(CONFIG_KEY_SRC);
    }

    public static void setDstConfigPath(Job j, String p) {
        j.put(CONFIG_KEY_DST, p);
    }

    public static String getDstConfigPath(Job j) {
        return j.get(CONFIG_KEY_DST);
    }

    public static void setPropertiesPath(Job j, String p) {
        j.put(PROPS, p);
    }

    public static String getPropertiesPath(Job j) {
        return j.get(PROPS);
    }

    public static void setResultingPlan(Job j, String p) {
        j.put(PLAN, p);
    }

    public static String getResultingPlan(Job j) {
        return j.get(PLAN);
    }

    public static void setMetrics(Job j, String p) {
        j.put(METRICS, p);
    }

    public static String getMetrics(Job j) {
        return j.get(METRICS);
    }

    public static void addVJobPath(Job j, String v) {
        String x = j.get(NBVJOBS);
        int next = x == null ? 0 : Integer.parseInt(x);
        next++;
        j.put(NBVJOBS, Integer.toString(next));
        j.put(VJOBS_PATH + next, v);
    }

    public static List<String> getVJobPaths(Job j) {
        String x = j.get(NBVJOBS);
        int nb = x == null ? 0 : Integer.parseInt(x);
        List<String> ls = new ArrayList<String>();

        for (int i = 1; i <= nb; i++) {
            String k = VJOBS_PATH + i;
            ls.add(j.get(k));
        }
        return ls;
    }
}
