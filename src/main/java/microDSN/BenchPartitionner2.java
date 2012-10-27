package microDSN;

import btrpsl.BtrPlaceVJobBuilder;
import btrpsl.constraint.ConstraintsCatalog;
import btrpsl.constraint.ConstraintsCatalogBuilderFromProperties;
import btrpsl.includes.PathBasedIncludes;
import entropy.PropertiesHelper;
import entropy.configuration.*;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.plan.Plan;
import entropy.plan.partitioner.OtherPartitioning;
import entropy.plan.partitioner.Partition;
import entropy.plan.partitioner.PartitioningException;
import entropy.template.DefaultVirtualMachineTemplateFactory;
import entropy.template.stub.StubVirtualMachineTemplate;
import entropy.vjob.*;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import gnu.trove.THashSet;
import microDSN.template.LargeEC2;
import microDSN.template.SmallEC2;
import microDSN.template.XLargeEC2;

import java.io.File;
import java.util.*;

/**
 * Bench related to the partitioning phase
 *
 * @author Fabien Hermenier
 */
public class BenchPartitionner2 {

    public static ManagedElementSet<VirtualMachine> createOrGet(Configuration big, Configuration cfg, ManagedElementSet<VirtualMachine> vms, String prefix) {
        ManagedElementSet<VirtualMachine> cpy = new SimpleManagedElementSet<VirtualMachine>();
        int nbCreated = 0;
        for (VirtualMachine vm : vms) {
            VirtualMachine v = big.getRunnings().get(prefix + vm.getName());
            if (v == null) {
                nbCreated++;
                v = vm.clone();
                v.rename(prefix + vm.getName());
                //Get its position on the cfg configuration and replicate it to the big configuration
                String oldName = cfg.getLocation(vm).getName();
                Node newNode = big.getOnlines().get(prefix + oldName);
                big.setRunOn(v, newNode);
            }
            cpy.add(v);
        }
        //System.err.println(nbCreated + "/" + vms.size()+ " "  +vms);
        return cpy;
    }

    public static List<VJob> duplicates(List<VJob> vjobs, Configuration big, Configuration cfg, String prefix) {
        List<VJob> cpy = new ArrayList<VJob>(vjobs.size());
        for (VJob v : vjobs) {
            VJob vjobCopy = new DefaultVJob(prefix + v.id());
            for (PlacementConstraint c : v.getConstraints()) {
                if (c instanceof ContinuousSpread) {
                    ManagedElementSet<VirtualMachine> vms = createOrGet(big, cfg, c.getAllVirtualMachines(), prefix);
                    vjobCopy.addConstraint(new ContinuousSpread(vms));
                } else if (c instanceof Fence) {
                    ManagedElementSet<VirtualMachine> vms = createOrGet(big, cfg, c.getAllVirtualMachines(), prefix);
                    ManagedElementSet<Node> ns = new SimpleManagedElementSet<Node>();
                    for (Node n : c.getNodes()) {
                        ns.add(big.getOnlines().get(prefix + n.getName()));
                    }
                    vjobCopy.addConstraint(new Fence(vms, ns));
                } else if (c instanceof Among) {
                    Among a = (Among) c;
                    ManagedElementSet<VirtualMachine> vms = createOrGet(big, cfg, c.getAllVirtualMachines(), prefix);

                    Set<ManagedElementSet<Node>> nss = new HashSet<ManagedElementSet<Node>>();
                    for (ManagedElementSet<Node> ns : a.getGroups()) {
                        ManagedElementSet<Node> x = new SimpleManagedElementSet<Node>();
                        for (Node n : ns) {
                            x.add(big.getOnlines().get(prefix + n.getName()));
                        }
                        nss.add(x);
                    }
                    vjobCopy.addConstraint(new Among(vms, nss));
                } else {
                    System.err.println("Unsupported constraint " + c);
                    System.exit(1);
                }
            }
            cpy.add(vjobCopy);
        }

        return cpy;
    }

    public static void benchPartitioning(BtrPlaceVJobBuilder vBuilder, String base, List<VJob> vjobs) throws Exception {


        //Read configurations, 1 by one
        File root = new File(base);
        Configuration big = new SimpleConfiguration();
        int nbRound = 0;
        List<VJob> all = new ArrayList<VJob>();
        for (File f : root.listFiles()) {
            Configuration cfg = FileConfigurationSerializerFactory.getInstance().read(f.getPath());
            //Shift cfg
            for (Node n : cfg.getOfflines()) {
                Node cpy = n.clone();
                cpy.rename(nbRound + "-" + n.getName());
                big.addOffline(cpy);
            }
            for (Node n : cfg.getOnlines()) {
                Node cpy = n.clone();
                cpy.rename(nbRound + "-" + n.getName());
                big.addOnline(cpy);
            }
            all.addAll(duplicates(vjobs, big, cfg, nbRound + "-"));
            nbRound++;
            System.out.println(nbRound + " cfg=" + big.getAllNodes().size() + " servers; " + big.getAllVirtualMachines().size() + " VMs" + " vjobs: " + all.size());
            System.err.println("-- start gc()");
            cfg = null;
            System.gc();
            System.gc();
            System.err.println("-- end gc()");
            part(big, all);
        }
    }

    public static void part(Configuration src, List<VJob> queue) {
        long st = System.currentTimeMillis();
        OtherPartitioning partitioner = new OtherPartitioning(src);
        List<PlacementConstraint> cs = new LinkedList<PlacementConstraint>();
        for (VJob v : queue) {
            for (PlacementConstraint c : v.getConstraints()) {
                if (!(c instanceof Fence)) {
                    cs.add(c);
                } else {
                    try {
                        partitioner.part((Fence) c);
                    } catch (PartitioningException e) {
                        Plan.logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        for (PlacementConstraint c : cs) {
            try {
                if (c instanceof Spread) {
                    partitioner.part((Spread) c);
                } else if (c instanceof Ban) {
                    partitioner.part((Ban) c);
                } else if (c instanceof Among) {
                    partitioner.part((Among) c);
                } else if (c instanceof Root) {
                    partitioner.part((Root) c);
                } else if (c instanceof Lonely) {
                    partitioner.part((Lonely) c);
                } else {
                    Plan.logger.warn("Unsupported constraint: " + c);
                }
            } catch (PartitioningException e) {
                Plan.logger.error(e.getMessage(), e);
            }
        }
        List<Partition> parts = partitioner.getResultingPartitions();
        long ed = System.currentTimeMillis();
        System.err.println((ed - st) + " " + parts.size());
    }

    private static BtrPlaceVJobBuilder makeVJobBuilder() throws Exception {
        DefaultVirtualMachineTemplateFactory tplFactory = new DefaultVirtualMachineTemplateFactory();
        tplFactory.add(new SmallEC2());
        tplFactory.add(new LargeEC2());
        tplFactory.add(new XLargeEC2());

        int[] cpu = {20, 30, 50, 60};
        int[] mem = {100, 200, 300};
        for (int c : cpu) {
            for (int m : mem) {
                StubVirtualMachineTemplate st = new StubVirtualMachineTemplate("c" + c + "m" + m, 1, c, m, new THashSet<String>());
                tplFactory.add(st);
            }
        }


        VJobElementBuilder eb = new DefaultVJobElementBuilder(tplFactory);
        ConstraintsCatalog cat = new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/btrpVjobs.properties")).build();
        return new BtrPlaceVJobBuilder(eb, cat);
    }

    public static void main(String[] args) {
        try {
            BtrPlaceVJobBuilder b = makeVJobBuilder();
            Configuration cfg = FileConfigurationSerializerFactory.getInstance().read(args[0] + File.separator + "0-src.pbd");
            b.setIncludes(new PathBasedIncludes(b, new File(args[1])));
            b.getElementBuilder().useConfiguration(cfg);
            List<File> files = listFiles(args[1], true);
            List<VJob> vjobs = new ArrayList<VJob>(files.size());
            for (File f : files) {
                vjobs.add(b.build(f));
            }
            benchPartitioning(b, args[0], vjobs);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    public static List<File> listFiles(String root, boolean recursive) {
        List<File> res = new ArrayList<File>();
        if (root != null) {
            File f = new File(root);
            if (f.exists() && f.isDirectory()) {
                for (File c : f.listFiles()) {
                    if (c.isFile()) {
                        res.add(c);
                    } else if (recursive && c.isDirectory()) {
                        res.addAll(listFiles(c.getPath(), true));
                    }
                }
            }
        }
        return res;
    }
}
