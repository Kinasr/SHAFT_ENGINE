package io.github.shafthq.shaft.listeners;

import com.shaft.driver.DriverFactory;
import io.github.shafthq.shaft.gui.image.ImageProcessingActions;
import io.github.shafthq.shaft.properties.PropertiesHelper;
import io.github.shafthq.shaft.tools.io.CheckpointCounter;
import io.github.shafthq.shaft.tools.io.ProjectStructureManager;
import io.github.shafthq.shaft.tools.io.ReportHelper;
import io.github.shafthq.shaft.tools.io.ReportManagerHelper;
import io.github.shafthq.shaft.tools.security.GoogleTink;
import org.testng.Reporter;

public class CucumberHelper {

    public static void shaftSetup() {
        if (Reporter.getCurrentTestResult() == null) {
            // running in native Cucumber mode
            System.setProperty("disableLogging", "true");
            PropertiesHelper.initialize();
            ProjectStructureManager.initialize();
            TestNGListenerHelper.configureJVMProxy();
            GoogleTink.initialize();
            GoogleTink.decrypt();
            System.setProperty("disableLogging", "false");

            ReportManagerHelper.logEngineVersion();
            ImageProcessingActions.loadOpenCV();

            ReportManagerHelper.initializeAllureReportingEnvironment();
            ReportManagerHelper.initializeExtentReportingEnvironment();

            ReportHelper.attachImportantLinks();
            ReportHelper.attachPropertyFiles();

            ReportManagerHelper.setDiscreteLogging(Boolean.parseBoolean(System.getProperty("alwaysLogDiscreetly")));
            ReportManagerHelper.setDebugMode(Boolean.valueOf(System.getProperty("debugMode")));
        }
    }

    public static void shaftTeardown() {
        if (Reporter.getCurrentTestResult() == null) {
            // running in native Cucumber mode
            DriverFactory.closeAllDrivers();

            ReportHelper.attachEngineLog();
            ReportHelper.attachExtentReport();
            ReportHelper.attachCucumberReport();
            CheckpointCounter.attach();
            ReportHelper.attachIssuesLog();

            ReportManagerHelper.setDiscreteLogging(true);
            JiraHelper.reportExecutionStatusToJira();
            GoogleTink.encrypt();
            ReportManagerHelper.generateAllureReportArchive();
            ReportManagerHelper.openAllureReportAfterExecution();
            ReportManagerHelper.logEngineClosure();
        }
    }
}
