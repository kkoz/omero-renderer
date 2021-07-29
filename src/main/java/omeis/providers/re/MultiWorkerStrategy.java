package omeis.providers.re;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ome.conditions.ResourceError;
import ome.io.nio.PixelBuffer;
import ome.model.core.Pixels;
import ome.model.display.ChannelBinding;
import ome.model.enums.PixelsType;
import ome.util.PixelData;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.data.PlaneDef;
import omeis.providers.re.data.PlaneFactory;
import omeis.providers.re.lut.LutReader;
import omeis.providers.re.quantum.QuantizationException;
import omeis.providers.re.quantum.QuantumStrategy;

public class MultiWorkerStrategy implements WorkerStrategy{
    
    /** The logger for this particular class */
    private static Logger log = LoggerFactory.getLogger(MultiWorkerStrategy.class);
    
    PixelShaderFactory shaderFactory;
    String shaderType;
    RGBBuffer dataBuffer;
    List<Plane2D> wData;
    RenderingStats performanceStats;
    int sizeX1;
    int sizeX2;
    int maxTasks;
    ExecutorService exservice;

    public MultiWorkerStrategy(PixelShaderFactory shaderFactory,
            String shaderType,
            RGBBuffer dataBuffer,
            List<Plane2D> wData,
            RenderingStats performanceStats,
            int sizeX1,
            int sizeX2,
            int maxTasks,
            ExecutorService exservice) {
        this.shaderFactory = shaderFactory;
        this.shaderType = shaderType;
        this.dataBuffer = dataBuffer;
        this.wData = wData;
        this.performanceStats = performanceStats;
        this.sizeX1 = sizeX1;
        this.sizeX2 = sizeX2;
        this.maxTasks = maxTasks;
        this.exservice = exservice;
    }
    
    public void work() throws Exception {
        // Process each active wavelength. If their number N > 1, then
        // process N-1 async and one in the current thread. If N = 1,
        // just use the current thread.
        Callable[] tasks = makeRenderingTasks();
        log.info("About to start rendering " + tasks.length + " tasks");
        performanceStats.startRendering();
        int n = tasks.length;
        Future[] rndTskFutures = new Future[n]; // [0] unused.

        while (0 < --n) {
            rndTskFutures[n] = exservice.submit(tasks[n]);
        }

        // Call the task in the current thread.
        if (n == 0) {
            tasks[0].call();
        }

        // Wait for all forked tasks (if any) to complete.
        for (n = 1; n < rndTskFutures.length; ++n) {
            try {
                rndTskFutures[n].get();
            } catch (Exception e) {
                if (e instanceof QuantizationException) {
                    throw (QuantizationException) e;
                }
                throw new RuntimeException(e);
            }
        }

        // Shutdown the task processor
        exservice.shutdown();

        // End the performance metrics for this rendering event.
        performanceStats.endRendering();
    }
    
    /**
     * Creates a set of rendering tasks for the image based on the calling
     * buffer type.
     * 
     * @param planeDef
     *            The plane to render.
     * @param buf
     *            The buffer to render into.
     * @return An array containing the tasks.
     */
    private MultiWorkerTask[] makeRenderingTasks() {
        List<MultiWorkerTask> tasks = new ArrayList<MultiWorkerTask>();
        // Create a number of rendering tasks.
        int taskCount = numTasks(sizeX2);
        int delta = sizeX2/taskCount;
        int x1Start = 0;
        int x1End = sizeX1;
        log.info("taskCount: "+taskCount+" delta: "+delta);
        for (int i = 0; i < taskCount; i++) {
                int x2Start = i*delta;
                int x2End = (i+1)*delta;
                tasks.add(new MultiWorkerTask(shaderFactory, shaderType, x1Start, x1End, x2Start, x2End, dataBuffer, wData));
        }

        // Turn the list into an array an return it.
        return tasks.toArray(new MultiWorkerTask[tasks.size()]);
    }
    
    
    /**
     * Retrieves the maximum number of reasonable tasks to schedule based on
     * image size and <i>maxTasks</i>.
     * 
     * @param size The width along the X2 axis.
     * @return the number of tasks to schedule.
     */
    private int numTasks(int size) {
        for (int i = maxTasks; i > 0; i--) {
            if (size % i == 0) {
                return i;
            }
        }
        return 1;
    }
}
