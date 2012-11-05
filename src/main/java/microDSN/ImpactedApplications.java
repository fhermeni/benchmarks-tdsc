package microDSN;

import btrpsl.BtrPlaceVJobBuilder;
import btrpsl.includes.PathBasedIncludes;
import entropy.configuration.*;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.configuration.parser.ProtobufConfigurationSerializer;
import entropy.vjob.VJob;
import entropy.vjob.builder.VJobBuilder;

import java.io.File;
import java.util.*;

/**
 * A tool to detect the applications impacted by a reconfiguration.
 * @author Fabien Hermenier
 */
public class ImpactedApplications {

    public static void main(String [] args) {

        if (args.length != 1) {
            System.out.println("Usage: ImpactedApplications input");
            System.out.println("input: the directory containing the workload");
            System.exit(1);
        }
        String base = args[0];

        int [] ratios = {3,4,5,6};
        double [] pcts = new double[ratios.length];
        try {
            for (int i  = 0; i < ratios.length; i++) {

                pcts[i] = impact(base + File.separator + "r" + ratios[i]);
            }
        } catch (Exception e) {
            System.err.print(e.getMessage());
            System.exit(1);
        }
        System.out.println("Ratio impactApps(%)");
        for (int i = 0; i < pcts.length; i++) {
            System.out.println(ratios[i] * 5 + " " + pcts[i]);
        }
    }

    private static double impact(String path) throws Exception{

        File vJobsPath = new File(path + File.separator + "c100p5000");
        File clientPath = new File(path + File.separator + "c100p5000" + File.separator + "clients");
        File configPath = new File(path + File.separator + "li");

        List<Configuration> configs = null;

        BtrPlaceVJobBuilder vb = null;
        List<VJob> vjobs = null;
        Map<String, VJob> vjobsById  =null;
            vb = MicroBenchHandler.makeVJobBuilder();
            PathBasedIncludes incls = new PathBasedIncludes(vb, vJobsPath);
            vb.setIncludes(incls);

            configs = getConfigurations(configPath.getPath());
            vjobs = getVJobs(vb, configs.get(0), clientPath);
            vjobsById = new HashMap<String, VJob>();
            for (VJob v : vjobs) {
                vjobsById.put(v.id(), v);
            }

        double avgImpactedApps = 0;
        double avgImpactedVMs = 0;
        for (Configuration cfg : configs) {
            ManagedElementSet<Node> saturated = Configurations.futureOverloadedNodes(cfg);
            ManagedElementSet<VirtualMachine> impactedVMs = cfg.getRunnings(saturated);

                Set<String> impactedVJobs = getImpactedVJobs(impactedVMs, vjobsById);
                avgImpactedApps += impactedVJobs.size();
                avgImpactedVMs += impactedVMs.size();
            //    System.out.println(cfg.getAllNodes().size() + " " + cfg.getAllVirtualMachines().size() + " " + vjobs.size() + " " + impactedVMs.size() + " " + impactedVJobs.size());
        }
        avgImpactedApps /= configs.size();
        avgImpactedVMs /= configs.size();
        int nbVMs = configs.get(0).getAllVirtualMachines().size();
        //System.out.println("avg. impacted VMs: " + avgImpactedVMs + "/" + nbVMs + " = " + (avgImpactedVMs / nbVMs * 100) + "%");
        //System.out.println("avg. impacted apps: " + avgImpactedApps + "/" + vjobs.size() + " = " + (avgImpactedApps / vjobs.size() * 100) + "%");
        return (avgImpactedApps / vjobs.size() * 100);

    }

    private static Set<String> getImpactedVJobs(ManagedElementSet<VirtualMachine> impactedVMs, Map<String, VJob> vjobsById) {
        Set<String> impacted = new HashSet<String>();
        for (VirtualMachine vm : impactedVMs) {
            //Get the associated VJob
            String n = vm.getName();
            String vjobId = n.substring(0, n.lastIndexOf('.'));
            VJob v = vjobsById.get(vjobId);
            if (v == null) {
                System.err.println("Unable to retrieve vjob '" + vjobId + "'");
            } else {
                impacted.add(v.id());
            }
        }
        return impacted;
    }


    private static List<Configuration> getConfigurations(String path) throws Exception {
        File f = new File(path);
        List<Configuration> cfgs = new ArrayList<Configuration>();
        if (f.isDirectory()) {
            for (File in : f.listFiles()) {
                    Configuration cfg = ProtobufConfigurationSerializer.getInstance().read(in.getPath());
                    cfgs.add(cfg);
            }
        }
        return cfgs;
    }

    private static List<VJob> getVJobs(VJobBuilder vb,Configuration cfg, File path) throws Exception {
        List<VJob> vjobs = new ArrayList<VJob>();
        vb.getElementBuilder().useConfiguration(cfg);
        for (File f : path.listFiles()) {
            VJob v = vb.build(f);
            //System.err.println("Read" + f.getPath());
            if (v.id().startsWith("clients")) {
                vjobs.add(v);
            }
        }

        return vjobs;
    }
}
