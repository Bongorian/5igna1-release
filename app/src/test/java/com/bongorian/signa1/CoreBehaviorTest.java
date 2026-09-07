package com.bongorian.signa1;

import org.junit.Test;

/** Independent fixtures exercise both distribution variants with identical expectations. */
public final class CoreBehaviorTest {
    @Test public void immutableStateAndRawRows() { EffectStateCheck.main(new String[0]); }
    @Test public void pipelineFixtures() { PipelineCheck.main(new String[0]); }
    @Test public void liveFaultBehavior() { FaultModelCheck.main(new String[0]); }
}
