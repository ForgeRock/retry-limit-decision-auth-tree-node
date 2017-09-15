/*
 * andy.hall@forgerock.com
 * Applies a configurable limit to a node to node connection
 */

package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;


@Node.Metadata(outcomeProvider = RetryLimitDecisionNode.OutcomeProvider.class,
        configClass = RetryLimitDecisionNode.Config.class)
public class RetryLimitDecisionNode implements Node {

    //smoff
    private final static String TRUE_OUTCOME_ID = "true";
    private final static String FALSE_OUTCOME_ID = "false";
    private static Integer loopCounter = 1;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The number of times to allow a retry.
         * @return the name.
         */
        @Attribute(order = 100)
        default String retryLimit() {
            return "3";
        }

    }

    private final Config config;

    /**
     * Create the node.
     * @param config The service config.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public RetryLimitDecisionNode(@Assisted Config config) throws NodeProcessException {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
	
	//How many times have we gone around this loop?
	if (Objects.equals(config.retryLimit(), String.valueOf(loopCounter))) {
        logger.info("RetryLimitDecisionNode limit reached");
        // reset loopCounter for next time.
        loopCounter = 1;
        return goTo(false).build();
    }
        else loopCounter++;

	//Limit not reached
	return goTo(true).build();

    }

    private Action.ActionBuilder goTo(boolean outcome) {
        return Action.goTo(outcome ? TRUE_OUTCOME_ID : FALSE_OUTCOME_ID);
    }

    static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        private static final String BUNDLE = RetryLimitDecisionNode.class.getName().replace(".", "/");

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString("trueOutcome")),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString("falseOutcome")));
        }
    }
}
