package org.pastalab.fray.junit

import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

class FrayEngineDescriptor(uniqueId: UniqueId) :
    EngineDescriptor(uniqueId, "fray (JUnit Platform)") {}
