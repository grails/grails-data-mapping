package org.grails.datastore.mapping.transactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;

import java.util.List;

/**
 * Extended version of {@link RuleBasedTransactionAttribute} that ensures all exception types are rolled back and allows inheritance of setRollbackOnly
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class CustomizableRollbackTransactionAttribute extends RuleBasedTransactionAttribute {

    private static final Logger log = LoggerFactory.getLogger(CustomizableRollbackTransactionAttribute.class);

    private static final long serialVersionUID = 1L;
    private boolean inheritRollbackOnly = true;

    public CustomizableRollbackTransactionAttribute() {
        super();
    }

    public CustomizableRollbackTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
        super(propagationBehavior, rollbackRules);
    }

    public CustomizableRollbackTransactionAttribute(org.springframework.transaction.interceptor.TransactionAttribute other) {
        super();
        setPropagationBehavior(other.getPropagationBehavior());
        setIsolationLevel(other.getIsolationLevel());
        setTimeout(other.getTimeout());
        setReadOnly(other.isReadOnly());
        setName(other.getName());
    }

    public CustomizableRollbackTransactionAttribute(TransactionDefinition other) {
        super();
        setPropagationBehavior(other.getPropagationBehavior());
        setIsolationLevel(other.getIsolationLevel());
        setTimeout(other.getTimeout());
        setReadOnly(other.isReadOnly());
        setName(other.getName());
    }

    public CustomizableRollbackTransactionAttribute(CustomizableRollbackTransactionAttribute other) {
        this((RuleBasedTransactionAttribute)other);
    }

    public CustomizableRollbackTransactionAttribute(RuleBasedTransactionAttribute other) {
        if(other instanceof CustomizableRollbackTransactionAttribute) {
            this.inheritRollbackOnly = ((CustomizableRollbackTransactionAttribute)other).inheritRollbackOnly;
        }
    }

    @Override
    public boolean rollbackOn(Throwable ex) {
        if (log.isTraceEnabled()) {
            log.trace("Applying rules to determine whether transaction should rollback on $ex");
        }

        RollbackRuleAttribute winner = null;
        int deepest = Integer.MAX_VALUE;

        List<RollbackRuleAttribute> rollbackRules = getRollbackRules();
        if (rollbackRules != null) {
            for (RollbackRuleAttribute rule : rollbackRules) {
                int depth = rule.getDepth(ex);
                if (depth >= 0 && depth < deepest) {
                    deepest = depth;
                    winner = rule;
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Winning rollback rule is: $winner" );
        }

        // User superclass behavior (rollback on unchecked) if no rule matches.
        if (winner == null) {
            log.trace("No relevant rollback rule found: applying default rules");

            // always rollback regardless if it is a checked or unchecked exception since Groovy doesn't differentiate those
            return true;
        }

        return !(winner instanceof NoRollbackRuleAttribute);
    }
    public boolean isInheritRollbackOnly() {
        return inheritRollbackOnly;
    }

    public void setInheritRollbackOnly(boolean inheritRollbackOnly) {
        this.inheritRollbackOnly = inheritRollbackOnly;
    }
}
