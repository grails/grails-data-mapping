<html>
    <head>
        <title>domain structure</title>
        <meta name="layout" content="main"/>
        <r:require module="jquery"/>
        <g:javascript>
            function toogleCheckboxes(name, value) {
                $("input[name=" + name + "]").each(function () {
                    this.checked = value;
                })
            };
        </g:javascript>
    </head>
<body>

<tmpl:nav/>

<div id="pageBody" class="body">

  <h2>Filtering</h2>
  <g:form name="domain" action="domain">
      <table>
        <tr>
            <td>Property types (display any of these)</td>
            <td>
              <g:each in="${filter}">
                <g:checkBox name="filter" value="${it}" checked="${filters.contains(it)}"/>${it}<br/>
              </g:each>
            </td>
            <td>
                <a href="#" onclick="toogleCheckboxes('filter',true)">all</a><br/>
                <a href="#" onclick="toogleCheckboxes('filter',false)">none</a>
            </td>
            <td>Domain classes</td>
            <td>
              <g:each in="${allDomainClasses}" var="dc">
                <g:checkBox name="domainClasses" value="${dc.name}" checked="${domainClassesNames.contains(dc.name)}"/>${dc.name}<br/>
              </g:each>
            </td>
          <td>
              <a href="#" onclick="toogleCheckboxes('domainClasses',true)">all</a><br/>
              <a href="#" onclick="toogleCheckboxes('domainClasses',false)">none</a>
          </td>
        </tr>
      </table>
    <g:submitButton name="update" value="Update" />
    </g:form>
<hr>
  
	        <h2>List of domain classes persisted via Neo4j</h2>
	        
        	<table>
        		<tr>
        			<th>class</th>
        			<th>name</th>
        			<th>type</th>
        			<th>assoc</th>
        			<th>bidi</th>
        			<th>hasOne</th>
        			<th>manyToMany</th>
        			<th>manyToOne</th>
        			<th>oneToMany</th>
        			<th>oneToOne</th>
        			<th>owning</th>
        			<th>circular</th>
        			<th>refName</th>
        			<th>refType</th>
        			<th>refDomCl</th>
        		</tr>
	        		<g:each in="${domainProps}" var="p" status="i">
	        			<tr class="${ (i % 2) == 0 ? 'even' : 'odd'}">
	        				<td>${p.domainClass.name}</td>
	        				<td>${p.name}</td>
	        				<td>${grails.util.GrailsNameUtils.getShortName(p.type.name)}</td>
	        				<td><img src="${p.association ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.association}"/></td>
	        				<td><img src="${p.bidirectional ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.bidirectional}"/></td>
	        				<td><img src="${p.hasOne ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.hasOne}"/></td>
	        				<td><img src="${p.manyToMany ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.manyToMany}"/></td>
	        				<td><img src="${p.manyToOne ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.manyToOne}"/></td>
	        				<td><img src="${p.oneToMany ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.oneToMany}"/></td>
	        				<td><img src="${p.oneToOne ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.oneToOne}"/></td>
	        				<td><img src="${p.owningSide ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.owningSide}"/></td>
	        				<td><img src="${p.circular ? resource(dir:'images',file: "true.png"): resource(dir:'images',file: "false.png")}" alt="${p.circular}"/></td>
	        				<td>${p.referencedPropertyName}</td>
	        				<td>${grails.util.GrailsNameUtils.getShortName(p.referencedPropertyType)}</td>
	        				<td>${p.referencedDomainClass}</td>
	        			</tr>
	        		</g:each>
	        </table>

		</div>
</body>
</html>