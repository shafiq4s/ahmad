package com.ahmad.quran.svgnative

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23,36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeSvgMeasurementsTest {
    @Test fun measureDirectSvgAndBoundedCacheModes() {
        val report = NativeSvgMeasurements.run(RuntimeEnvironment.getApplication(),
            "Robolectric host JVM; API 23 rendering is command execution only, API 36 uses native Skia")
        assertEquals(2,report.getJSONArray("cacheModes").length())
        assertTrue(report.getDouble("firstParseMs")>0)
        File(System.getProperty("nativeSvg.outputDir"),"measurements-robolectric-api-${android.os.Build.VERSION.SDK_INT}.json")
            .writeText(report.toString(2)+"\n")
    }
}
