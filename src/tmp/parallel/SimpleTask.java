package tmp.parallel;

import java.util.Random;

public class SimpleTask implements Schedulable {

    double[] ans;
    int curID;
    static final int N = 1000000;
    int nTasks;

    public SimpleTask(int nTasks) {
        curID = -1;
        this.nTasks = nTasks;
        ans = new double[2];
    }

    public void showResult() {
        System.out.println(ans[0] + " " + ans[1]);
    }

    public Object compute(int taskID) {
        Random r = new Random(taskID);
        int a = r.nextInt();
        for (int i = 0; i < 10000; i++) {
            for (int j = 0; j < 10000; j++) {
                for (int k = 0; k < 2; k++) {
                    a = taskID + i + j + k + r.nextInt();
                }
            }
        }

        double[] result = new double[2];
        result[0] = a;
        result[1] = 2;
        return result;
    }

    public int getNumCompletedTasks() {
        return curID;
    }
    
    public int getNumTasks() {
        return nTasks;
    }
    
    public synchronized int fetchCurrTaskID() {
        if (curID < nTasks) {
            curID++;
        }
        return curID;
    }
    
    public synchronized void update(Object partialResult) {
        double[] res = (double[]) partialResult;
        ans[0] += res[0];
        ans[1] += res[1];
        //showResult();
    }
}