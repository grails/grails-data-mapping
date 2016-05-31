package org.grails.plugins.web.rx.mvc

import grails.artefact.Controller
import grails.artefact.controller.RestResponder
import grails.async.web.AsyncGrailsWebRequest
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.plugins.web.async.GrailsAsyncContext
import org.grails.web.errors.GrailsExceptionResolver
import org.grails.web.servlet.mvc.ActionResultTransformer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.util.WebUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.async.AsyncWebRequest
import org.springframework.web.context.request.async.WebAsyncManager
import org.springframework.web.context.request.async.WebAsyncUtils
import org.springframework.web.servlet.ModelAndView
import rx.Observable
import rx.Subscriber

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Transforms an RX result into async dispatch
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
@Slf4j
class RxResultTransformer implements ActionResultTransformer, Controller, RestResponder {

    @Autowired(required = false)
    GrailsExceptionResolver exceptionResolver

    @Override
    Object transformActionResult(GrailsWebRequest webRequest, String viewName, Object actionResult) {
        if(actionResult instanceof Observable) {
            Observable observable = (Observable)actionResult

            final request = webRequest.getCurrentRequest()
            WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request)
            final response = webRequest.getResponse()

            AsyncWebRequest asyncWebRequest = new AsyncGrailsWebRequest(request, response, webRequest.servletContext)
            asyncManager.setAsyncWebRequest(asyncWebRequest)

            asyncWebRequest.startAsync()
            request.setAttribute(GrailsApplicationAttributes.ASYNC_STARTED, true)
            def asyncContext = asyncWebRequest.asyncContext
            asyncContext = new GrailsAsyncContext(asyncContext, webRequest)
            asyncContext.start {

                List rxResults = []

                observable
                    .switchIfEmpty(Observable.create({ Subscriber s ->
                            s.onNext(null)
                            s.onCompleted()
                    } as Observable.OnSubscribe))
                        .subscribe(new Subscriber() {
                    @Override
                    void onCompleted() {
                        GrailsWebRequest rxWebRequest = new GrailsWebRequest((HttpServletRequest)asyncContext.request, (HttpServletResponse)asyncContext.response, request.getServletContext())
                        WebUtils.storeGrailsWebRequest(rxWebRequest)

                        try {
                            if(rxResults.isEmpty()) {
                                respond((Object)null)
                            }
                            else {
                                if(rxResults.size() == 1) {
                                    def first = rxResults.get(0)
                                    respond first
                                }
                                else {
                                    respond rxResults
                                }
                            }
                            def modelAndView = asyncContext.getRequest().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                            if(modelAndView != null) {
                                asyncContext.dispatch()
                            }
                            else {
                                asyncContext.complete()
                            }
                        } finally {
                            rxWebRequest.requestCompleted()
                            WebUtils.clearGrailsWebRequest()
                        }
                    }

                    @Override
                    void onError(Throwable e) {
                        if(!asyncContext.response.isCommitted()) {
                            def httpServletResponse = (HttpServletResponse) asyncContext.response
                            if(exceptionResolver != null) {

                                def modelAndView = exceptionResolver.resolveException((HttpServletRequest) asyncContext.request, httpServletResponse, this, (Exception) e)
                                asyncContext.getRequest().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, modelAndView);
                                asyncContext.dispatch()

                            }
                            else {
                                log.error("Async Dispatch Error: ${e.message}", e)
                                httpServletResponse.sendError(500, "Async Dispatch Error: ${e.message}")
                                asyncContext.complete()
                            }
                        }
                    }

                    @Override
                    void onNext(Object o) {
                        rxResults.add(o)
                    }
                })
            }
            return null
        }
        return actionResult
    }
}
