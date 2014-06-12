<html>
    <head>
        <title>Neo4j Statistics</title>
		<meta name="layout" content="main" />
	</head>
<body>

<tmpl:nav/>



<div id="pageBody" class="body">
	        <h2>Counters</h2>

            <h3>Nodes</h3>
            <table>
                <g:each in="${typeCounter}" var="item" status="i">
                <tr class="${ (i % 2) == 0 ? 'even' : 'odd'}">
                    <td>${item.key}</td>
                    <td>${item.value}</td>
                </tr>
                 </g:each>
                <tr>
                    <th>sum:</th>
                    <td>${typeCounter.values().sum()}</td>
                </tr>
        	</table>
            <h3>Relationships</h3>
            <table>
                <g:each in="${reltypeCounter}" var="item" status="i">
                <tr class="${ (i % 2) == 0 ? 'even' : 'odd'}">
                    <td>${item.key}</td>
                    <td>${item.value}</td>
                </tr>
                 </g:each>
                <tr>
                    <th>sum:</th>
                    <td>${reltypeCounter.values().sum()}</td>
                </tr>
            </table>

		</div>
</body>
</html>