package org.kie.dmn.xls2dmn;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.kie.api.builder.Message.Level;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
import org.kie.dmn.core.internal.utils.DMNRuntimeBuilder;
import org.kie.dmn.core.util.DMNRuntimeUtil;
import org.kie.dmn.feel.util.Either;
import org.kie.dmn.validation.DMNValidatorFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardApprovalTest {

    private static final Logger LOG = LoggerFactory.getLogger(CardApprovalTest.class);

    private DMNRuntime getDMNRuntimeWithCLI() throws Exception {
        File tempFile = File.createTempFile("xls2dmn", ".dmn");
        App.main(new String[]{"src/test/resources/Card_approval.xlsx", tempFile.toString()});

        List<DMNMessage> validate = DMNValidatorFactory.newValidator().validate(tempFile);
        assertThat(validate.stream().filter(m -> m.getLevel()==Level.ERROR).count(), is(0L));

        Either<Exception, DMNRuntime> fromResources = DMNRuntimeBuilder.fromDefaults()
                         .buildConfiguration()
                         .fromResources(Arrays.asList(ResourceFactory.newFileResource(tempFile)));

        LOG.info("{}", System.getProperty("java.io.tmpdir"));
        LOG.info("{}", tempFile);
        DMNRuntime dmnRuntime = fromResources.getOrElseThrow(RuntimeException::new);
        return dmnRuntime;
    }

    @Test
    public void testCLI() throws Exception {
        final DMNRuntime dmnRuntime = getDMNRuntimeWithCLI();
        DMNModel dmnModel = dmnRuntime.getModels().get(0);

        DMNContext dmnContext = dmnRuntime.newContext();
        dmnContext.set("Annual Income", 70);
        dmnContext.set("Assets", 150);
        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, dmnContext);
        LOG.debug("{}", dmnResult);
        assertThat(DMNRuntimeUtil.formatMessages(dmnResult.getMessages()), dmnResult.hasErrors(), is(false));
        assertThat(dmnResult.getDecisionResultByName("Standard card score").getResult(), is(new BigDecimal(562)));
        assertThat(dmnResult.getDecisionResultByName("Gold card score").getResult(), is(new BigDecimal(468)));
    }
}