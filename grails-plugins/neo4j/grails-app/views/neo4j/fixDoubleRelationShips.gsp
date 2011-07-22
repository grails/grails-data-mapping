<html>
    <head>
        <title>Neo4j Statistics</title>
		<meta name="layout" content="grails" />
	</head>
<body>

<tmpl:nav/>

<div id="pageBody" class="body">
  <h2>Counters</h2>
  # removed relationships: ${removed}

  <%-- <table>
      <g:each in="${doubledRelationships}" var="item" status="i">
      <tr class="${ (i % 2) == 0 ? 'even' : 'odd'}">
          <td>${item.key}</td>
          <td>${item.value}</td>
      </tr>
       </g:each>
  </table> --%>

</div>
</body>
</html>