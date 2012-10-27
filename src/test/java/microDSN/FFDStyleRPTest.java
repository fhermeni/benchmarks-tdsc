package microDSN;

import btrpsl.BtrPlaceVJobBuilder;
import btrpsl.constraint.ConstraintsCatalog;
import btrpsl.constraint.ConstraintsCatalogBuilderFromProperties;
import btrpsl.includes.PathBasedIncludes;
import entropy.PropertiesHelper;
import entropy.configuration.*;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import entropy.plan.Plan;
import entropy.plan.TimedReconfigurationPlan;
import entropy.plan.choco.CustomizableSplitablePlannerModule;
import entropy.plan.durationEvaluator.MockDurationEvaluator;
import entropy.template.DefaultVirtualMachineTemplateFactory;
import entropy.vjob.VJob;
import entropy.vjob.builder.DefaultVJobElementBuilder;
import entropy.vjob.builder.VJobElementBuilder;
import microDSN.template.LargeEC2;
import microDSN.template.SmallEC2;
import microDSN.template.XLargeEC2;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 22/06/12
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */
@Test
public class FFDStyleRPTest {

    public static List<VJob> getVJobs(Configuration cfg, File root, List<File> vJobPaths) throws Exception {
        BtrPlaceVJobBuilder b = makeVJobBuilder();
        PathBasedIncludes incls = new PathBasedIncludes(b, root);
        b.setIncludes(incls);
        List<VJob> vJobs = new ArrayList<VJob>();
        b.getElementBuilder().useConfiguration(cfg);
        for (File path : vJobPaths) {
            VJob v = b.build(path);
            vJobs.add(v);
        }
        return vJobs;
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
            } else {
                System.err.println(f.getPath() + " " + f.exists() + " " + f.isDirectory());
            }
        }
        return res;
    }

    private static BtrPlaceVJobBuilder makeVJobBuilder() throws Exception {
        DefaultVirtualMachineTemplateFactory tplFactory = new DefaultVirtualMachineTemplateFactory();
        tplFactory.add(new SmallEC2());
        tplFactory.add(new LargeEC2());
        tplFactory.add(new XLargeEC2());
        VJobElementBuilder eb = new DefaultVJobElementBuilder(tplFactory);
        ConstraintsCatalog cat = new ConstraintsCatalogBuilderFromProperties(new PropertiesHelper("config/btrpVjobs.properties")).build();
        return new BtrPlaceVJobBuilder(eb, cat);
    }


    public void test1() {
        try {
            BtrPlaceVJobBuilder builder = makeVJobBuilder();

            String root = "../dsn-repair-10pct-3";
            //String root = "../dsn-restart-05pct-3";
            String id = "p1000c100";
            List<Configuration> srcs = new ArrayList<Configuration>();
            Configuration src = FileConfigurationSerializerFactory.getInstance().read(root + "/configs/0-src.pbd");
            for (Node n : src.getAllNodes()) {
                n.setMemoryCapacity(6000);
            }
            srcs.add(src);
            for (int i = 0; i < 50; i++) {
                Configuration c = FileConfigurationSerializerFactory.getInstance().read(root + "/configs/" + i + "-src.pbd");
                srcs.add(c);
            }


            List<File> files = listFiles(root + "/" + id, true);

            List<VJob> vjobs = getVJobs(src, new File(root + "/" + id), files);
            MockDurationEvaluator ev = new MockDurationEvaluator(1, 2, 3, 4, 5, 6, 7, 8, 9);


            FFDStyleRP rp1 = new FFDStyleRP(ev);
            rp1.setRepairMode(true);
            FFDStyleVMAffinityRP rp2 = new FFDStyleVMAffinityRP(ev);
            rp2.setRepairMode(true);
            FFDStyleVMAndHostAffinityRP rp3 = new FFDStyleVMAndHostAffinityRP(ev);
            rp3.setRepairMode(true);
            CustomizableSplitablePlannerModule rp4 = new CustomizableSplitablePlannerModule(ev);
            rp4.setTimeLimit(3);
            rp4.setRepairMode(true);
            rp4.setPartitioningMode(CustomizableSplitablePlannerModule.PartitioningMode.sequential);


            List<Plan> approaches = new ArrayList<Plan>();
            approaches.add(rp1);
            approaches.add(rp2);
            approaches.add(rp3);
            approaches.add(rp4);

            for (Configuration c : srcs) {
                System.err.println("---New config---");
                List<TimedReconfigurationPlan> solutions = new ArrayList<TimedReconfigurationPlan>();

                for (Plan plan : approaches) {
                    try {
                        ManagedElementSet<VirtualMachine> toRun = src.getRunnings().clone();
                        toRun.addAll(src.getWaitings());
                        TimedReconfigurationPlan p = plan.compute(c,
                                toRun,
                                new SimpleManagedElementSet<VirtualMachine>(),
                                src.getSleepings(),
                                new SimpleManagedElementSet<VirtualMachine>(),
                                src.getOnlines(),
                                src.getOfflines(),
                                vjobs
                        );
                        solutions.add(p);
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                        solutions.add(null);
                    }

                }

                for (int i = 0; i < approaches.size(); i++) {
                    System.err.print(approaches.get(i).getClass().getSimpleName() + " ");
                    System.err.println(solutions.get(i) == null ? "null" : (solutions.get(i).getActions().size() + " action(s)"));
                    //System.err.println(solutions.get(i));
                }
                System.err.flush();
            }
            Assert.fail();
        } catch (Exception e) {
            Assert.fail(e.getMessage(), e);

        }


    }
}
