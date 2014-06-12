<html>
    <head>
        <title>explore node space</title>
		<meta name="layout" content="main" />
	</head>
<body>

<tmpl:nav/>

<div id="pageBody" class="body">
    <h1>Showing relationship (${rel.id})</h1>
        <table>
          <tr>
            <td>start node:</td>
            <td><g:link action="node" id="${rel.startNode.id}">${rel.startNode.id}</g:link> (type: ${rel.startNode.getProperty("type","n/a")})</td>
          </tr>
          <tr>
            <td>end node:</td>
            <td><g:link action="node" id="${rel.endNode.id}">${rel.endNode.id}</g:link> (type: ${rel.endNode.getProperty("type","n/a")})</td>
          </tr>
        </table>
    <h3>properties</h3>
    <table border="1">
      <tr><th>property name</th><th>property value</th><th>value type</th></tr>
      <g:each in="${rel.propertyKeys}">
          <tr>
            <td>${it}</td>
            <td>${rel.getProperty(it)}</td>
            <td>${rel.getProperty(it)?.getClass().name}</td>
          </tr>
      </g:each>
    </table>
  </div>
  </body>
</html>
