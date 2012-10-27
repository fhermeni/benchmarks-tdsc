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

import entropy.jobsManager.CommitedJobHandler;
import entropy.jobsManager.Job;
import entropy.jobsManager.JobDispatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author Fabien Hermenier
 */
public class MicroBenchDispatcher implements CommitedJobHandler {

    private String output;

    private String root;

    private JobDispatcher master;

    private BlockingQueue<String> detailQueue;

    public static final int QUEUE_SIZE = 100;

    public MicroBenchDispatcher(String rcBase, String output, int p) throws IOException {
        master = new JobDispatcher(p, rcBase, this);
        this.output = output;
        this.root = rcBase;
        this.detailQueue = new LinkedBlockingDeque<String>(QUEUE_SIZE);
    }

    public void enqueue(File cfgs, File vjobsPath, String props) {
        List<String[]> configs = new ArrayList<String[]>();
        List<String> vjobs = new ArrayList<String>();
        for (File entry : cfgs.listFiles()) {
            if (entry.isFile()) {
                if (entry.getName().contains("-src")) {
                    String[] fs = new String[2];
                    fs[0] = entry.getPath();
                    fs[1] = entry.getPath(); //If there is no destination configuration the src is also the destiation
                    String ext = fs[0].substring(fs[0].lastIndexOf('.'));
                    File f = new File(entry.getParent() + File.separator + entry.getName().substring(0, entry.getName().indexOf('-')) + "-dst" + ext);
                    if (f.exists()) {
                        fs[1] = f.getPath();
                    }
                    configs.add(fs);
                }
            }
        }
        if (vjobsPath != null) {
            for (File entry : list(vjobsPath)) {
                vjobs.add(entry.getPath());
            }
        }

        int id = 0;
        for (String[] entries : configs) {
            Job j = new Job(id++);
            PlanJob.setSrcConfigPath(j, entries[0]);
            PlanJob.setDstConfigPath(j, entries[1]);
            PlanJob.setPropertiesPath(j, new File(props).getPath());
            j.put("incl", vjobsPath.getPath());
            for (String v : vjobs) {
                PlanJob.addVJobPath(j, v);
            }
            master.enqueue(j);
        }
    }

    private List<File> list(File path) {
        List<File> files = new ArrayList<File>();
        for (File entry : path.listFiles()) {
            if (entry.isFile()) {
                files.add(entry);
            } else if (entry.isDirectory()) {
                files.addAll(list(entry));
            }
        }
        return files;
    }

    public void start() {
        master.getLogger().info(this.master.getWaitings().size() + " jobs enqueued");
        new Thread() {
            public void run() {
                while (true) {
                    flushQueue();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        JobDispatcher.getLogger().warn(e.getMessage());
                    }
                }
            }
        }.start();
        master.run();
    }

    private void printDetails(Job j) {
        String plan = PlanJob.getResultingPlan(j);
        if (plan != null) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(new FileWriter(new StringBuilder(output).append('/').append(j.getId()).append(".txt").toString()));
                out.println(plan);
            } catch (IOException e) {
                JobDispatcher.getLogger().error(e.getMessage(), e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private void printResult(Job j) {
        try {
            detailQueue.put(PlanJob.getMetrics(j));
        } catch (InterruptedException e) {
            JobDispatcher.getLogger().warn(e.getMessage());
        }
    }

    public void flushQueue() {
        List<String> lines = new ArrayList<String>();
        int nb = detailQueue.drainTo(lines);
        try {
            PrintWriter out = new PrintWriter(new FileWriter(output + "/results.data", true), true);
            for (String line : lines) {
                out.println(line);
            }
            out.close();
        } catch (IOException e) {
            JobDispatcher.getLogger().error(e.getMessage(), e);
        }

    }

    @Override
    public void jobCommited(Job j) {
        printDetails(j);
        printResult(j);
    }


    public static void usage(int code) {
        System.err.println("Usage: MicroBenchDispatcher [-p port] -cfgs configDir [-vjobs vjobDir] -props properties -o output");
        System.exit(code);
    }

    public static void fatal(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    public static void main(String[] args) {

        if (args.length <= 6) {
            usage(1);
        }

        int port = JobDispatcher.DEFAULT_PORT;

        String configDir = null;
        String vjobsDir = null;
        String output = null;
        String props = null;

        for (int i = 0; i < args.length; i++) {
            String p = args[i];
            if ("-h".equals(p)) {
                usage(0);
            } else if ("-p".equals(p)) {
                port = Integer.parseInt(args[++i]);
            } else if ("-cfgs".equals(p)) {
                configDir = args[++i];
            } else if ("-vjobs".equals(p)) {
                vjobsDir = args[++i];
            } else if ("-props".equals(p)) {
                props = args[++i];
            } else if ("-o".equals(p)) {
                output = args[++i];
            } else {
                fatal("Unknown parameter: " + p);
            }
        }

        if (configDir == null) {
            fatal("Missing parameter '-cfgs'");
        }

        if (output == null) {
            fatal("Missing parameter '-o'");
        }
        if (props == null) {
            fatal("Missing parameter '-props'");
        }

        File configFile = new File(configDir);
        File vjobsFile = null;
        if (vjobsDir != null) {
            vjobsFile = new File(vjobsDir);
        }
        File propsFile = new File(props);

        if (!configFile.isDirectory()) {
            fatal(configFile.getName() + " must be an existing directory");
        }
        if (vjobsFile != null && !vjobsFile.isDirectory()) {
            fatal(vjobsFile.getName() + " must be an existing directory");
        }
        if (!propsFile.isFile()) {
            fatal(propsFile.getName() + " must be an existing file");
        }

        File outputDir = new File(output);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            fatal("Unable to create directory '" + output + "'");
        }
        try {
            String root = System.getProperty("user.dir");
            System.err.println("Serving file from '" + root + "'");
            MicroBenchDispatcher m = new MicroBenchDispatcher(root, output, port);
            m.enqueue(configFile, vjobsFile, props);
            m.start();


        } catch (Exception e) {
            fatal(e.getMessage());
        }
    }
}
