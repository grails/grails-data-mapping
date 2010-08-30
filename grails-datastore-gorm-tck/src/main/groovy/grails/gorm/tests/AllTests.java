package grails.gorm.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 10:14:07 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(AllTests.TestSuite.class)
@Suite.SuiteClasses({CriteriaBuilderTests.class,
                     CrudOperationsTests.class,
                     OrderByTests.class,
                     ProxyLoadingTests.class,
                     QueryByAssociationTests.class,
                     RangeQueryTests.class,
                     ValidationTests.class,
                     WithTransactionTests.class})
public class AllTests {

    public AllTests() {
    }


    public static class TestSuite extends Suite  {

        public TestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
            super(klass, TestRunnerBuilder.INSTANCE);
            TestRunnerBuilder.INSTANCE.setTestSuite(this);
        }

        private static class TestRunnerBuilder extends RunnerBuilder {

            private TestSuite testSuite;
            public static final TestRunnerBuilder INSTANCE = new TestRunnerBuilder();

            public void setTestSuite(TestSuite testSuite) {
                this.testSuite = testSuite;
            }

            @Override
            public Runner runnerForClass(Class<?> aClass) throws Throwable {
                final BlockJUnit4ClassRunner runner = new BlockJUnit4ClassRunner(aClass) {
                    @Override
                    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
                        List<FrameworkMethod> befores= getTestClass().getAnnotatedMethods(
                                Before.class);

                        FrameworkMethod frameworkMethod =  getTestSuiteMethod("setUp");

                        if(frameworkMethod != null)
                            befores.add(frameworkMethod);
                        return new RunBefores(statement, befores, target);
                    }

                    @Override
                    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
                        List<FrameworkMethod> befores= getTestClass().getAnnotatedMethods(
                                After.class);

                        FrameworkMethod frameworkMethod =  getTestSuiteMethod("tearDown");
                       
                        if(frameworkMethod != null)
                            befores.add(frameworkMethod);
                        return new RunBefores(statement, befores, target);
                    }

                    private FrameworkMethod getTestSuiteMethod(String name) {
                        try {
                            final Method declaredMethod = testSuite.getClass().getDeclaredMethod(name, new Class[0]);
                            declaredMethod.setAccessible(true);
                            return new FrameworkMethod(declaredMethod) {
                                @Override
                                public Object invokeExplosively(Object target, Object... params) throws Throwable {
                                    return super.invokeExplosively(testSuite, params);
                                }
                            };
                        } catch (NoSuchMethodException e) {
                        }
                        return null;
                    }
                };

                return runner;
            }
        }

        @Override
        protected void runChild(Runner runner, RunNotifier notifier) {
            super.runChild(runner, new DelegatingRunNotifier(notifier) {
                @Override
                public void fireTestStarted(Description description) throws StoppedByUserException {
                    super.fireTestStarted(description);
                    setUp();
                }

                @Override
                public void fireTestFinished(Description description) {
                    super.fireTestFinished(description);
                    tearDown();
                }
            });
        }


        protected void tearDown() {
            //To change body of created methods use File | Settings | File Templates.
        }

        protected void setUp() {
            //To change body of created methods use File | Settings | File Templates.
        }

        private class DelegatingRunNotifier extends RunNotifier {
            private RunNotifier delegate;

            public DelegatingRunNotifier(RunNotifier notifier) {
                this.delegate =  notifier;
            }

            @Override
            public void addListener(RunListener listener) {
                delegate.addListener(listener);
            }

            @Override
            public void removeListener(RunListener listener) {
                delegate.removeListener(listener);
            }

            @Override
            public void fireTestRunStarted(Description description) {
                delegate.fireTestRunStarted(description);
            }

            @Override
            public void fireTestRunFinished(Result result) {
                delegate.fireTestRunFinished(result);
            }

            @Override
            public void fireTestStarted(Description description) throws StoppedByUserException {
                delegate.fireTestStarted(description);
            }

            @Override
            public void fireTestFailure(Failure failure) {
                delegate.fireTestFailure(failure);
            }

            @Override
            public void fireTestAssumptionFailed(Failure failure) {
                delegate.fireTestAssumptionFailed(failure);
            }

            @Override
            public void fireTestIgnored(Description description) {
                delegate.fireTestIgnored(description);
            }

            @Override
            public void fireTestFinished(Description description) {
                delegate.fireTestFinished(description);
            }

            @Override
            public void pleaseStop() {
                delegate.pleaseStop();
            }

            @Override
            public void addFirstListener(RunListener listener) {
                delegate.addFirstListener(listener);
            }
        }
    }



}
