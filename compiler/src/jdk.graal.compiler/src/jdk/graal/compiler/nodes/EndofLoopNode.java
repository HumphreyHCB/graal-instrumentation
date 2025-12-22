package jdk.graal.compiler.nodes;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_32;
import static jdk.graal.compiler.nodeinfo.NodeSize.SIZE_1;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.graph.NodeClass;
import jdk.graal.compiler.graph.NodeSourcePosition;
import jdk.graal.compiler.nodeinfo.NodeInfo;
import jdk.graal.compiler.nodes.spi.LIRLowerable;
import jdk.graal.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Emits nops, functions as a lir marker for RDTSC instrumentation in the LIR phase for Bubo.
 */
// @formatter:off
@NodeInfo(cycles = CYCLES_0,
          cyclesRationale = "",
          size = SIZE_1)
// @formatter:on
public final class EndofLoopNode extends FixedWithNextNode implements LIRLowerable {

    public static final NodeClass<EndofLoopNode> TYPE = NodeClass.create(EndofLoopNode.class);
    
    public final int LOOP_ID;
    public final NodeSourcePosition position;

    public EndofLoopNode(int loopId, NodeSourcePosition position) {
        super(TYPE, StampFactory.forVoid());
        this.LOOP_ID = loopId;
        this.position = position;

    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // the stamp for this need need to be checked
        generator.getLIRGeneratorTool().emitLoopEnd(LOOP_ID, position);
    }
}
