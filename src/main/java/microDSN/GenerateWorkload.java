/*
 * Copyright (c) Fabien Hermenier
 *
 *         This file is part of Entropy.
 *
 *         Entropy is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU Lesser General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or
 *         (at your option) any later version.
 *
 *         Entropy is distributed in the hope that it will be useful,
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *
 *         GNU Lesser General Public License for more details.
 *         You should have received a copy of the GNU Lesser General Public License
 *         along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package microDSN;

import btrpsl.BtrPlaceVJobBuilder;
import btrpsl.BtrpPlaceVJobBuilderException;
import btrpsl.constraint.ConstraintsCatalog;
import btrpsl.constraint.ConstraintsCatalogBuilderFromProperties;
import btrpsl.includes.PathBasedIncludes;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import entropy.PropertiesHelper;
import entropy.configuration.*;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.plan.durationEvaluator.DurationEvaluator;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.DefaultVirtualMachineTemplateFactory;
import entropy.vjob.Fence;
import entropy.vjob.PlacementConstraint;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import instancesMaker.ConfigurationAlterer;
import instancesMaker.Generator;
import instancesMaker.VJobAlterer;
import microDSN.template.LargeEC2;
import microDSN.template.SmallEC2;
import microDSN.template.XLargeEC2;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Fabien Hermenier
 */
public class GenerateWorkload {

    private final static int sizeSwitch = 250;

    private static final Random rnd = new Random();

    public final static String BTRP_TEMPLATE1 = new StringBuilder(100)
            .append("import datacenter;\n\n")
            .append("VM[1..10]: smallEC2<clone,boot=5,halt=5>;\n")
            .append("VM[11..15]: largeEC2<clone,boot=5,halt=5>;\n")
            .append("VM[16..20]: xLargeEC2<clone,boot=60,halt=10>;\n")
            .append('\n')
            .append("$T1 = VM[1..10];\n")
            .append("$T2 = VM[11..15];\n")
            .append("$T3 = VM[16..20];\n")
            .append('\n')
            .append("for $t in $T[1..3] {\n")
            .append("\tspread($t);\n")
            .append("}\n")
            .append("among($T3, $leafs);\n")
            .append("export $me to *;").toString();

    public final static String BTRP_TEMPLATE2 = new StringBuilder(100)
            .append("import datacenter;\n\n")
            .append("VM[1..10]: smallEC2<clone,boot=5,halt=5>;\n")
            .append("VM[11..15]: largeEC2<clone,boot=5,halt=5>;\n")
            .append("VM[16..20]: xLargeEC2<clone,boot=60,halt=10>;\n")
            .append('\n')
            .append("$T1 = VM[1..10];\n")
            .append("$T2 = VM[11..15];\n")
            .append("$T3 = VM[16..20];\n")
            .append("export $me to *;")
            .append('\n').toString();

    /**
     * Instantiate a template.
     *
     * @return {@code true} if the folder and the vjobs have been created
     * @throws IOException if an error occurred while writing the btrplint scripts
     */
    public static List<VJob> load(BtrPlaceVJobBuilder builder, List<File> files) throws BtrpPlaceVJobBuilderException, IOException {
        List<VJob> vjobs = new ArrayList<VJob>(files.size());
        for (File f : files) {
            vjobs.add(builder.build(f));
        }
        return vjobs;
    }

    /**
     * Instantiate a template.
     *
     * @param content  the content of the btrplint file
     * @param nsPrefix the root of the namespace
     * @param idPrefix the root of the vjob identifier
     * @param nb       the number of instances
     * @param output   the output folder. Will be created if non-existant
     * @return {@code true} if the folder and the vjobs have been created
     * @throws IOException if an error occurred while writing the btrplint scripts
     */
    public static List<File> instantiate(String content, String nsPrefix, String idPrefix, int nb, int startId, String output) throws IOException {
        String root = new StringBuilder(output).append(File.separator).append(nsPrefix).toString();
        File folder = new File(root);
        String path = folder.getPath();
        List<File> res = new ArrayList<File>(nb);
        if (!folder.exists() && !folder.mkdirs()) {
            return res;
        }
        for (int i = startId; i < startId + nb; i++) {
            String id = idPrefix + i;
            String ns = new StringBuilder(nsPrefix).append('.').append(id).toString();
            BufferedWriter out = null;
            File f = new File(new StringBuilder(path).append(File.separator).append(id).append(".btrp").toString());

            try {
                out = new BufferedWriter(new FileWriter(f));
                StringBuilder b = new StringBuilder(100);
                b.append("namespace ");
                b.append(ns);
                b.append(";\n");
                b.append(content);
                out.write(b.toString());
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            res.add(f);
        }
        return res;
    }

    private static Configuration createInfra(int nbNodes, int nbCPUs, int capaCPU, int capaMem) {
        Configuration cfg = new SimpleConfiguration();
        for (int i = 1; i <= nbNodes; i++) {
            Node n = new SimpleNode("N" + i, nbCPUs, capaCPU, capaMem);
            cfg.addOnline(n);
        }
        return cfg;
    }

    private static void storeInfra(Configuration cfg, String outputFolder, int sizeSwitch) throws IOException {
        int nbNodes = cfg.getOnlines().size();
        PrintWriter out = null;
        try {
            out = new PrintWriter(outputFolder + File.separator + "datacenter.btrp");
            out.println("namespace datacenter;");
            out.print("$leafs = @N[1..");
            out.print(nbNodes);
            out.print("] % ");
            out.print(sizeSwitch);
            out.println(";\n");
            out.println("export $leafs to *;\n");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void storeInfraMngt(String outputFolder) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File(outputFolder + File.separator + "sysadmin.btrp"));
            out.println("namespace sysadmin;");
            //VM for btrPlace
            out.println("import datacenter;");
            out.println("vmBtrPlace : largeEC2;");
            out.println("root(vmBtrPlace);");
            out.println("lonely(vmBtrPlace);");
            out.println("export $me to *;");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void storePartition(String outputFolder, Configuration cfg, List<VJob> vjobs, int sizePart, int nbPerPart) throws Exception {
        int nbNodes = cfg.getAllNodes().size();
        int nbPart = nbNodes / sizePart;
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File(outputFolder + File.separator + "part" + sizePart + ".btrp"));
            out.println("namespace part" + sizePart + ";");
            out.println("import datacenter;");
            out.println("import clients.*;");
            out.println("import sysadmin;");
            int startIdx = 1;
            //System.err.println("sysadmin is fenced into @N[1.." + (startIdx + sizePart - 1) + "]");
            out.println("fence($sysadmin,@N[1.." + (startIdx + sizePart - 1) + "]);");
            for (int x = 0; x < vjobs.size(); x += nbPerPart) {
                for (int k = 0; k < nbPerPart; k++) {
                    VJob v = vjobs.get(x + k);
                    String id = v.id();
                    if (id.startsWith("clients")) {
                        //System.err.println(id + " is fenced into @N[" + startIdx + ".." + Math.min((startIdx + sizePart - 1), nbNodes) + "]");
                        out.println("fence($" + id + ", @N[" + startIdx + ".." + Math.min((startIdx + sizePart - 1), nbNodes) + "]);");
                    }
                }
                startIdx = (startIdx + sizePart) % nbNodes;
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void storePartition2(String outputFolder, Configuration cfg, int nbApps, int sizePart, int nbPerPart) throws Exception {
        int nbNodes = cfg.getAllNodes().size();
        int nbPart = nbNodes / sizePart;
        PrintWriter out = null;
        try {
            out = new PrintWriter(new File(outputFolder + File.separator + "part.btrp"));
            out.println("namespace part" + sizePart + ";");
            out.println("import datacenter;");
            out.println("import clients.*;");
            out.println("import sysadmin;");
            int startIdx = 1;
            //System.err.println("sysadmin is fenced into @N[1.." + (startIdx + sizePart - 1) + "]");
            out.println("fence($sysadmin,@N[1.." + (startIdx + sizePart - 1) + "]);");
            for (int x = 1; x <= nbApps; x += nbPerPart) {
                for (int k = 0; k < nbPerPart; k++) {
                    //VJob v = vjobs.get(x + k);
                    String id = "clients.c" + (x + k);
                    if (id.startsWith("clients")) {
                        //System.err.println(id + " is fenced into @N[" + startIdx + ".." + Math.min((startIdx + sizePart - 1), nbNodes) + "]");
                        out.println("fence($" + id + ", @N[" + startIdx + ".." + Math.min((startIdx + sizePart - 1), nbNodes) + "]);");
                    }
                }
                startIdx = (startIdx + sizePart) % nbNodes;
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * An infrastructure with a consolidation ratio of 6:1
     * with 30,000 VMs and 5,000 nodes. The partition size varies from 250 to 5000
     */
    private static void scalabilityBench2(BtrPlaceVJobBuilder builder, DurationEvaluator ev, String base, int nbInstances, double ratio, boolean onlyRestarts) throws Exception {
        System.out.println("-- Mode: " + (onlyRestarts ? "restart" : "repair") + "; Ratio= " + ratio + "%" + "; " + nbInstances + " instance(s) per point; Output is " + base + " --");
        int nbNodes = 5000;
        int nbApps = 1500;
        SplitableVJobLauncher plan = new SplitableVJobLauncher(ev);
        plan.setPartitioningMode(SplitableVJobLauncher.PartitioningMode.sequential);
        String root = base;


        //Generate the configurations and "standard vjobs"
        int size = 5000;
        int nbApp = 1500;
        File f = new File(root + "/vjobs_100");
        f.mkdirs();
        builder.setIncludes(new PathBasedIncludes(builder, f));

        Configuration cfg = createInfra(size, 1, 150, 6400);
        builder.getElementBuilder().useConfiguration(cfg);

        storeInfra(cfg, f.getPath(), sizeSwitch);
        storeInfraMngt(f.getPath());

        long st = System.currentTimeMillis();
        List<VJob> allClients = load(builder, instantiate(BTRP_TEMPLATE1, "clients", "c", nbApp, 1, f.getPath()));

        long ed = System.currentTimeMillis();
        System.out.println("Time to read and parse " + allClients.size() + " script(s): " + (ed - st) + "ms");
        ManagedElementSet<VirtualMachine> allVMs = new SimpleManagedElementSet<VirtualMachine>();
        for (VJob v : allClients) {
            allVMs.addAll(v.getVirtualMachines());
        }

        System.out.println("Content: " + allVMs.size() + " VM(s); " + cfg.getAllNodes().size() + " node(s)");
        //Add the partitioning. Round robin for the vjobs
        int nbPartitions = cfg.getAllNodes().size() / sizeSwitch;

        List<ManagedElementSet<Node>> partitions = new ArrayList<ManagedElementSet<Node>>(nbPartitions);
        for (int x = 0; x < nbPartitions; x++) {
            partitions.add(new SimpleManagedElementSet<Node>());
        }
        int nbPart = 0;
        for (int nIdx = 0; nIdx < cfg.getAllNodes().size(); nIdx++) {
            partitions.get(nbPart).add(cfg.getAllNodes().get(nIdx));
            if (partitions.get(nbPart).size() == sizeSwitch) {
                nbPart++;
            }
        }
        int pIdx = 0;
        for (VJob v : allClients) {
            PlacementConstraint pf = new Fence(v.getVirtualMachines(), partitions.get(pIdx++ % nbPartitions));
            v.addConstraint(pf);
        }
        VJob sysadmin = builder.build(new File(f.getPath() + File.separator + "sysadmin.btrp"));
        List<VJob> allVJobs = new ArrayList<VJob>();
        allVJobs.addAll(allClients);

        sysadmin.addConstraint(new Fence(sysadmin.getVirtualMachines(), partitions.get(0)));
        allVJobs.add(sysadmin);

        Configuration x = null;
        System.out.println("Generating " + nbInstances + " instance(s):");
        for (int j = 0; j < nbInstances; j++) {
            if (j % 5 == 0) {
                VJobAlterer.setRandomCPUDemand(0.2, 1.0, allClients);
                //Set the initial load
                System.out.print('x');
                x = Generator.generate(plan, cfg.getOnlines(), cfg.getOfflines(), allVJobs);
                if (x == null) {
                    System.err.println("Unable to generate a base configuration");
                    System.exit(1);
                }
                VJobAlterer.setCPUConsumptionToDemand(allClients);
                if (!onlyRestarts) {
                    setNonViable(allClients, ratio / 100);
                }
            } else {
                System.out.print('.');
            }
            System.out.flush();

            //Just a pinch of chaos
            Configuration c = x.clone();
            ConfigurationAlterer.shuffle(c, allVJobs, 100);
            if (j == 80) {
                System.out.println();
            }

            ManagedElementSet<VirtualMachine> toRun = null;
            if (onlyRestarts) {
                toRun = ConfigurationAlterer.applyNodeFailureRatio(c, ratio / 100);
                for (VirtualMachine vm : toRun) {
                    c.addWaiting(vm);
                }
            }
            FileConfigurationSerializerFactory.getInstance().write(c, root + "/configs/" + j + "-src.pbd");
            for (VJob v : allClients) {
                for (PlacementConstraint cst : v.getConstraints()) {
                    if (!cst.isSatisfied(cfg)) {
                        System.err.println(cst + " is not satisfied");
                        System.exit(1);
                    }
                }
            }

            if (onlyRestarts) {
                for (VirtualMachine vm : toRun) {
                    c.setRunOn(vm, c.getOnlines().get(0));
                }
                FileConfigurationSerializerFactory.getInstance().write(c, root + "/configs/" + j + "-dst.pbd");
            }
        }
        System.out.println();
        statistics(x);

        //Now, we generate for each partition size, different includes varying by the % of constraints.
        int[] simplifications = new int[]{0, 33, 66, 100};
        int[] partSize = new int[]{250, 500, 1000, 2500, 5000};
        for (int pSize : partSize) {
            System.out.println("-- Partition bench size=" + pSize + "--");
            for (int s : simplifications) {
                System.out.print("Storing scripts with " + s + "% of them having constraints: ");
                System.out.flush();
                st = System.currentTimeMillis();
                int nbWoConstraints = (int) Math.ceil(1d * allClients.size() * ((100 - (1f * s)) / 100));
                f = new File(root + "/p" + pSize + "c" + s);
                f.mkdirs();
                instantiate(BTRP_TEMPLATE2, "clients", "c", nbWoConstraints, 1, f.getPath());
                instantiate(BTRP_TEMPLATE1, "clients", "c", allClients.size() - nbWoConstraints, nbWoConstraints + 1, f.getPath());
                storeInfra(cfg, f.getPath(), sizeSwitch);
                storeInfraMngt(f.getPath());

                storePartition2(f.getPath(), cfg, nbApps, pSize, pSize / partSize[0]);

                ed = System.currentTimeMillis();
                System.out.println((ed - st) + " ms");
                System.gc();
                System.gc();
            }
        }
    }

    /**
     * An infrastructure with a consolidation ratio of 6:1
     * Size of the infrastructure vary from 2k to of 1k nodes, we vary the number of applications: 200, 300 and 400. Each is composed of
     * 20 VMs. So the resulting consolidations ratios are 4:1, 6:1 and 8:1. CPU consumption is picked up randomly
     * for the demand, 50% of the applications have its demand increased by 20% (max is 100%)
     */
    private static void methodBench(BtrPlaceVJobBuilder builder, DurationEvaluator ev, String base, int nbInstances, double ratio, boolean onlyRestarts) throws Exception {
        System.out.println("-- Method Mode: " + (onlyRestarts ? "restart" : "repair") + "; Ratio= " + ratio + "%" + "; " + nbInstances + " instance(s) per point; Output is " + base + " --");

        int[] sizes = {250, 500, 1000, 2500, 5000};
        int[] nbApps = {75, 150, 300, 750, 1500};

        SplitableVJobLauncher plan = new SplitableVJobLauncher(ev);
        plan.setPartitioningMode(SplitableVJobLauncher.PartitioningMode.sequential);
        String root = base;
        for (int idx = 0; idx < nbApps.length; idx++) {
            int size = sizes[idx]/*5000*/;
            int nbApp = nbApps[idx];

            //Generate the configurations and "standard vjobs"
            File f = new File(root + "/vjobs_100");
            f.mkdirs();
            builder.setIncludes(new PathBasedIncludes(builder, f));

            Configuration cfg = createInfra(size, 1, 150, 1600);
            builder.getElementBuilder().useConfiguration(cfg);

            storeInfra(cfg, f.getPath(), sizeSwitch);
            storeInfraMngt(f.getPath());

            long st = System.currentTimeMillis();
            List<VJob> allClients = load(builder, instantiate(BTRP_TEMPLATE1, "clients", "c", nbApp, 1, f.getPath()));
            long ed = System.currentTimeMillis();

            System.out.println("Time to read and parse " + allClients.size() + " script(s): " + (ed - st) + "ms");
            ManagedElementSet<VirtualMachine> allVMs = new SimpleManagedElementSet<VirtualMachine>();
            for (VJob v : allClients) {
                allVMs.addAll(v.getVirtualMachines());
            }

            System.out.println("Content: " + allVMs.size() + " VM(s); " + cfg.getAllNodes().size() + " node(s)");
            //Add the partitioning. Round robin for the vjobs
            int nbPartitions = cfg.getAllNodes().size() / sizeSwitch;

            List<ManagedElementSet<Node>> partitions = new ArrayList<ManagedElementSet<Node>>(nbPartitions);
            for (int x = 0; x < nbPartitions; x++) {
                partitions.add(new SimpleManagedElementSet<Node>());
            }
            int nbPart = 0;
            for (int nIdx = 0; nIdx < cfg.getAllNodes().size(); nIdx++) {
                partitions.get(nbPart).add(cfg.getAllNodes().get(nIdx));
                if (partitions.get(nbPart).size() == sizeSwitch) {
                    nbPart++;
                }
            }
            int pIdx = 0;
            for (VJob v : allClients) {
                PlacementConstraint pf = new Fence(v.getVirtualMachines(), partitions.get(pIdx++ % nbPartitions));
                v.addConstraint(pf);
            }
            VJob sysadmin = builder.build(new File(f.getPath() + File.separator + "sysadmin.btrp"));
            List<VJob> allVJobs = new ArrayList<VJob>();
            allVJobs.addAll(allClients);

            sysadmin.addConstraint(new Fence(sysadmin.getVirtualMachines(), partitions.get(0)));
            allVJobs.add(sysadmin);
            //

            Configuration x = null;
            System.out.println("Generating " + nbInstances + " instance(s):");
            for (int j = 0; j < nbInstances; j++) {
                if (j % 5 == 0) {
                    VJobAlterer.setRandomCPUDemand(0.2, 1.0, allClients);
                    //Set the initial load
                    System.out.print('x');
                    x = Generator.generate(plan, cfg.getOnlines(), cfg.getOfflines(), allVJobs);
                    if (x == null) {
                        System.err.println("Unable to generate a base configuration");
                        System.exit(1);
                    }
                    VJobAlterer.setCPUConsumptionToDemand(allClients);
                    if (!onlyRestarts) {
                        setNonViable(allClients, ratio / 100);
                    }
                } else {
                    System.out.print('.');
                }
                System.out.flush();

                //Just a pinch of chaos
                Configuration c = x.clone();
                ConfigurationAlterer.shuffle(c, allVJobs, 100);
                if (j == 80) {
                    System.out.println();
                }
                ManagedElementSet<VirtualMachine> toRun = null;
                if (onlyRestarts) {
                    toRun = ConfigurationAlterer.applyNodeFailureRatio(c, ratio / 100);
                    for (VirtualMachine vm : toRun) {
                        c.addWaiting(vm);
                    }
                }
                FileConfigurationSerializerFactory.getInstance().write(c, root + "/configs_" + nbApp + "/" + j + "-src.pbd");
                for (VJob v : allClients) {
                    for (PlacementConstraint cst : v.getConstraints()) {
                        if (!cst.isSatisfied(cfg)) {
                            System.err.println(cst + " is not satisfied");
                            System.exit(1);
                        }
                    }
                }

                if (onlyRestarts) {
                    for (VirtualMachine vm : toRun) {
                        c.setRunOn(vm, c.getOnlines().get(0));
                    }
                    FileConfigurationSerializerFactory.getInstance().write(c, root + "/configs_" + nbApp + "/" + j + "-dst.pbd");
                }
            }
            System.out.println();
            statistics(x);

            //Now, we generate for each partition size, different includes varying by the % of constraints.
            int[] simplifications = new int[]{0, 33, 66, 100};
            for (int s : simplifications) {
                System.out.print("Storing scripts with " + s + "% of them having constraints: ");
                System.out.flush();
                st = System.currentTimeMillis();
                int nbWoConstraints = (int) Math.ceil(1d * allClients.size() * ((100 - (1f * s)) / 100));
                f = new File(root + "/a" + nbApp + "c" + s);
                f.mkdirs();
                instantiate(BTRP_TEMPLATE2, "clients", "c", nbWoConstraints, 1, f.getPath());
                instantiate(BTRP_TEMPLATE1, "clients", "c", allClients.size() - nbWoConstraints, nbWoConstraints + 1, f.getPath());
                storeInfra(cfg, f.getPath(), sizeSwitch);
                storeInfraMngt(f.getPath());
                ed = System.currentTimeMillis();
                System.out.println((ed - st) + " ms");
                System.gc();
                System.gc();
            }
        }
    }

    /**
     * For a specific use case
     * Generate a root with datacenter, vjobs, sysadmin and a part file
     *
     * @param builder
     * @param ev
     * @param base
     * @throws Exception
     */
    private static void partitionBench(BtrPlaceVJobBuilder builder, String base) throws Exception {
        int[] partSize = new int[]{250, 500, 1000, 2500};
        int nbNodes = 5000;
        int nbApp = 1500;

        Configuration cfg = createInfra(nbNodes, 1, 150, 6400);
        String root = base;

        for (int i = 0; i < partSize.length; i++) {
            int pSize = partSize[i];
            System.err.println("-- Partition bench size=" + pSize + "--");
            File f = new File(root + "/" + nbApp + "/part" + partSize[i]);
            f.mkdirs();
            builder.setIncludes(new PathBasedIncludes(builder, f));

            builder.getElementBuilder().useConfiguration(cfg);
            storeInfra(cfg, f.getPath(), sizeSwitch);
            storeInfraMngt(f.getPath());

            List<VJob> vjobs = load(builder, instantiate(BTRP_TEMPLATE1, "clients", "c", nbApp, 1, f.getPath()));
            storePartition2(f.getPath(), cfg, vjobs.size(), pSize, pSize / partSize[0]);
        }
    }

    /**
     * Change the demand to have a non-viable configuration
     *
     * @param vjobs
     * @param ratio the ratio of vjobs that must have their demand increased by 30% (at most)
     */
    private static void setNonViable(List<VJob> vjobs, double ratio) {
        int nbToModify = (int) (ratio * vjobs.size());
        while (nbToModify > 0) {
            nbToModify--;
            int idx = rnd.nextInt(vjobs.size());
            VJob v = vjobs.get(idx);
            double r = VJobAlterer.getCPUDemandRatio(v);
            r = r >= 0.8 ? 1.0 : (r + 0.3);
            VJobAlterer.setCPUDemandRatio(v, r);
        }
    }

    private static void statistics(Configuration cfg) {
        System.out.println("- CPU -");
        System.out.println("usage: " + (100 * ConfigurationAlterer.getCPUConsumptionLoad(cfg)) + "%" +
                " demand: " + (100 * ConfigurationAlterer.getCPUDemandLoad(cfg)) + "%" +
                " memory: " + (100 * ConfigurationAlterer.getMemoryConsumptionLoad(cfg)) + "%");
    }

    public static void main(String[] args) {
        DurationEvaluator durations = new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9);
        ChocoLogging.setVerbosity(Verbosity.SILENT);
        if (args.length != 4) {
            System.err.println("Usage: generator (repair|restart) ratio nbInstances outputFolder");
            System.exit(1);
        }

        String mode = args[0];
        double ratio = Double.parseDouble(args[1]);
        int nbInstances = Integer.parseInt(args[2]);
        String output = args[3];
        if (!mode.equals("repair") && !mode.equals("restart")) {
            System.err.println("Mode must be either 'restart' or 'repair'. Currently '" + mode + "'");
            System.exit(1);
        }

        File f = new File(output);
        if (f.exists()) {
            System.err.println(output + " already exists");
            System.exit(1);
        }

        try {

            /*if (args.length != 2) {
                System.err.println("Usage: BenchPartitionner configs_path vjobs-path");
            } */
            DefaultVirtualMachineTemplateFactory tplFactory = new DefaultVirtualMachineTemplateFactory();
            tplFactory.add(new SmallEC2());
            tplFactory.add(new LargeEC2());
            tplFactory.add(new XLargeEC2());
            VJobElementBuilder eb = new DefaultVJobElementBuilder(tplFactory);
            ConstraintsCatalog cat = new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/btrpVjobs.properties")).build();
            BtrPlaceVJobBuilder b = new BtrPlaceVJobBuilder(eb, cat);

            //scalabilityBench2(b, durations, output, nbInstances, ratio, mode.equals("restart"));
            //methodBench(b, durations, output, nbInstances, ratio, mode.equals("restart"));
            //qualityBench(b, durations, output, nbInstances, ratio, mode.equals("restart"));


        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }
}