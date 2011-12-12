<html>
    <head>
        <title>explore node space</title>
		<meta name="layout" content="main" />
	</head>
<body>

<tmpl:nav/>

<div id="pageBody" class="body">
    <h1>Showing node (${node.id})</h1>
    <h3>Node properties</h3>
    <table border="1">
      <tr><th>property name</th><th>property value</th><th>value type</th></tr>
      <g:each in="${node.propertyKeys}">
          <tr>
            <td>${it}</td>
            <td>${node.getProperty(it)}</td>
            <td>${node.getProperty(it)?.getClass().name}</td>
          </tr>
      </g:each>
    </table>
    <h3>Relationships</h3>
    <table border="1">
      <tr><th>id</th><th>type</th><th>properties</th><th>direction</th><th>target</th></tr>
      <g:each in="${node.relationships}" var="rel">
          <tr>
            <td><g:link action="relationship" id="${rel.id}">${rel.id}</g:link></td>
            <td>${rel.type.name()}</td>
	    <td><g:each in="${rel.propertyKeys}" var="prop">${prop} = ${rel.getProperty(prop)}<br/> </g:each></td>
            <td><g:formatBoolean boolean="${rel.startNode==node}" true="outgoing" false="incoming"/></td>
            <td><g:set var="other" value="${rel.getOtherNode(node)}"/> <g:link id="${other.id}">${other}</g:link></td>

          </tr>
      </g:each>

    </table>
</div>
  </body>
</html>
