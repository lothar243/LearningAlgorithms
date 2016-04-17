import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by jeff on 4/16/16.
 */
public class NeuralNetTest extends NeuralNet {
    public static final double EPSILON = Math.pow(10, -12);
    @Test
    public void testFeedForwardBackPropagate1() throws Exception {
        double[][][] weights = new double[3][1][2];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                weights[i][0][j] = .05;
            }
        }
        NeuralNet net = new NeuralNet(weights);
        net.learningRate = .001;
        assertEquals(0.518979, net.feedForward(new double[]{1})[0], .0001);
        net.backPropagate(new double[]{0.0});
        assertEquals(.05 - (6.72474*Math.pow(10, -5)), net.nodes[2][0].inputWeights[0], .0000001);
        assertEquals(.05 - (8.48955*Math.pow(10, -7)), net.nodes[1][0].inputWeights[0], .0000001);
        assertEquals(.05 - (2.016356*Math.pow(10, -8)), net.nodes[0][0].inputWeights[0], .0000001);
    }

    @Test
    public void testFeedForwardBackPropagate2() throws Exception {
        double[][][] weights = new double[][][]
                { {     {.05, .05}, {.05, .05}},
                        {{.05, .05, .05}, {.05, .05, .05}},
                        {{.05, .05, .05}, {.05, .05, .05}}};


        NeuralNet net = new NeuralNet(weights);
        net.learningRate = .001;
        double[] feedFordwardOutput = net.feedForward(new double[]{1, 1});
        assertEquals(.52562, feedFordwardOutput[0], .00001);
        assertEquals(.52562, feedFordwardOutput[1], .00001);

        net.backPropagate(new double[]{0,0});
        System.out.println(net.toString());
        // layer 2
        assertEquals(.05 - .003444, net.nodes[2][0].inputWeights[0], .000001);
        assertEquals(.05 - .003444, net.nodes[2][0].inputWeights[1], .000001);
        assertEquals(.05 - .00655297, net.nodes[2][0].inputWeights[2], .000001);
        assertEquals(.05 - .003444, net.nodes[2][1].inputWeights[0], .000001);
        assertEquals(.05 - .003444, net.nodes[2][1].inputWeights[1], .000001);
        assertEquals(.05 - .00655297, net.nodes[2][1].inputWeights[2], .000001);
        // layer 1
        assertEquals(.05 - .0000857788677, net.nodes[1][1].inputWeights[0], .000001);
        assertEquals(.05 - .0000857788677, net.nodes[2][1].inputWeights[1], .000001);
        assertEquals(.05 - .00016339, net.nodes[2][1].inputWeights[2], .000001);
        // layer 0
        assertEquals(.05 - .000004074674, net.nodes[2][0].inputWeights[1], .000001);
        assertEquals(.05 - .000004074674, net.nodes[2][0].inputWeights[2], .000001);
        assertEquals(.05 - .000004074674, net.nodes[2][0].inputWeights[3], .000001);
    }

    @Test
    public void testBackPropagate3() throws Exception {
        double[][][] weights = new double[][][]
                {{  {.15, .2, .35}, {.25, .3, .35}},
                        {{.4, .45, .6}, {.5, .55, .6}}};
        NeuralNet net = new NeuralNet(weights);
        net.learningRate = .5;

        System.out.println(net.toString());
        double[] feedForwardOutput = net.feedForward(new double[]{.05, .1});
        assertEquals(.75136507, feedForwardOutput[0], .0000001);
        assertEquals(.772928465, feedForwardOutput[1], .0000001);

        net.backPropagate(new double[]{.01, .99});
        assertEquals(.35891648, net.nodes[1][0].inputWeights[0], .0000001);
        assertEquals(.408666186, net.nodes[1][0].inputWeights[1], .0000001);
        assertEquals(.511301270, net.nodes[1][1].inputWeights[0], .0000001);
        assertEquals(.561370121, net.nodes[1][1].inputWeights[1], .0000001);

        assertEquals(.149780716, net.nodes[0][0].inputWeights[0], .0000001);
        assertEquals(.19956143, net.nodes[0][0].inputWeights[1], .0000001);
        assertEquals(.24975114, net.nodes[0][1].inputWeights[0], .0000001);
        assertEquals(.29950229, net.nodes[0][1].inputWeights[1], .0000001);

    }
}