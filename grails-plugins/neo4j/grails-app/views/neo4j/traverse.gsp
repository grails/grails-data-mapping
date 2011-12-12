<%@ page contentType="text/html;charset=UTF-8" %>
<html>
  <head>
  	<title>Simple GSP page</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
  </head>
  <body>
  <tmpl:nav/>
            <g:if test="${flash.message}">
            <div class="message">${flash.message}</div>
            </g:if>
            <g:hasErrors bean="${command}">
            <div class="errors">
                <g:renderErrors bean="${command}" as="list" />
            </div>
            </g:hasErrors>

    <g:form action="traverse">
        <table>
          <tr class="value ${hasErrors(bean:command,field:'id','errors')}">
            <td>Node (id)</td>
            <td><input type="text" name="id" value="${fieldValue(bean:command, field:'id')}"/></td>
          </tr>
          <tr class="value ${hasErrors(bean:command,field:'order','errors')}">
            <td>Traverseorder</td>
            <td><g:select name="order" value="${fieldValue(bean:command, field:'order')}" from="${['BREADTH_FIRST', 'DEPTH_FIRST']}"/></td>
          </tr>
          <tr class="value ${hasErrors(bean:command,field:'stopEvaluator','errors')}">
            <td>Stopevaluator</td>
            <td><g:textArea name="stopEvaluator" value="${fieldValue(bean:command, field:'stopEvaluator')}" rows="5" cols="40"/></td>
          </tr>
          <tr class="value ${hasErrors(bean:command,field:'returnableEvaluator','errors')}">
            <td>ReturnableEvaluator</td>
            <td><g:textArea name="returnableEvaluator" value="${fieldValue(bean:command, field:'returnableEvaluator')}" rows="5" cols="40"/></td>
          </tr>

        </table>
        <g:submitButton name="traverse" value="Traverse" />
    </g:form>
  </body>
</html>
