package tmp.parallel;

public class Tester {

    // Simple test program
    public static void main(String[] args) throws Exception {
        

        if (args.length == 0) {
            System.out.println("Usage: java Scheduler nTasks nThreads");
            System.out.println("  Expected observation:");
            System.out.println("  TotalRunningTime ~= SingleTaskTime * (nTasks/nThreads)");
            System.exit(0);
        }

        Timer.start();
        (new SimpleTask(1)).compute(0);
        Timer.record("single");
        
        Timer.start();
        int policy = Scheduler.STATIC_UNIFORM_ALLOCATE;
//        policy = Scheduler.DYNAMIC_NEXT_AVAILABLE;
        (new Scheduler(new SimpleTask(Integer.parseInt(args[0])), Integer.parseInt(args[1]), policy)).run();
        Timer.record("thread");
        Timer.report();
    }
    
}
