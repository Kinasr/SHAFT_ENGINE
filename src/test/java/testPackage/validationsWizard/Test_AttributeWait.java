package testPackage.validationsWizard;

import com.shaft.driver.SHAFT;
import org.openqa.selenium.By;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class Test_AttributeWait {
    private SHAFT.GUI.WebDriver driver;

    private final String URL = "https://the-internet.herokuapp.com/dynamic_loading/1";
    private final By buttonStart = By.cssSelector("#start button");
    private final By divFinish = By.id("finish");

    @BeforeMethod
    void setup() {
        driver = new SHAFT.GUI.WebDriver();
    }

    @Test
    void testWaitToAttribute() {
        driver.browser()
                .navigateToURL(URL);
        driver.element()
                .click(buttonStart)
                .waitToAttribute(divFinish, "style", "");

        driver.assertThat()
                .element(divFinish)
                .isVisible()
                .perform();
    }

    @Test(expectedExceptions = AssertionError.class)
    void testWaitToAttributeWithWrongAttribute() {
        driver.browser()
                .navigateToURL(URL);
        driver.element()
                .click(buttonStart)
                .waitToAttribute(divFinish, "name", "test");
    }

    @Test(expectedExceptions = AssertionError.class)
    void testWaitToAttributeWithWrongAttributeValue() {
        driver.browser()
                .navigateToURL(URL);
        driver.element()
                .click(buttonStart)
                .waitToAttribute(divFinish, "style", "test");
    }

    @AfterMethod
    void tearDown() {
        driver.quit();
    }
}
