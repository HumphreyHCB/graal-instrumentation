package jdk.graal.compiler.nodes;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_0;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static jdk.graal.compiler.nodeinfo.NodeCycles.CYCLES_32;
import static jdk.graal.compiler.nodeinfo.NodeSize.SIZE_1;

import jdk.graal.compiler.core.common.type.StampFactory;
import jdk.graal.compiler.graph.NodeClass;
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
public final class StartofGraphNode extends FixedWithNextNode implements LIRLowerable {

    public static final NodeClass<StartofGraphNode> TYPE = NodeClass.create(StartofGraphNode.class);
    

    public StartofGraphNode() {
        super(TYPE, StampFactory.forVoid());

    }

    @Override
    public void generate(NodeLIRBuilderTool generator) {
        // the stamp for this need need to be checked
        generator.getLIRGeneratorTool().emitGraphStart();
    }
}
