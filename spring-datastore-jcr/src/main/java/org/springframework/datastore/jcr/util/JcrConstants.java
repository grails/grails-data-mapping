package org.springframework.datastore.jcr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public interface JcrConstants {
   public static String DEFAULT_JCR_TYPE = "nt:unstructured";
   public static final String MIXIN_REFERENCEABLE = "mix:referenceable";
   public static final String MIXIN_VERSIONABLE = "mix:versionable";
   public static final String MIXIN_LOCKABLE = "mix:lockable";    
}
