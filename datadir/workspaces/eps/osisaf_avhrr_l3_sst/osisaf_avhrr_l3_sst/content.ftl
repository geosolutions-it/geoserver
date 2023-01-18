{"type":"Feature","id":"","geometry":null,"properties":
    <#list features as feature>
    {
        <#list feature.attributes as attribute>
            <#if !attribute.isGeometry>
                "${attribute.name?js_string}": "${attribute.value?js_string}"<#if attribute_has_next>,</#if>
            </#if>
        </#list>
    }
    <#if feature_has_next>,</#if>
    </#list>

}

