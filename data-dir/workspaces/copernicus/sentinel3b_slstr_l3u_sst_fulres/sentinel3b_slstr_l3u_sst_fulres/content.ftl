<#-- 
Body section of the GetFeatureInfo template, it's provided with one feature collection, and
will be called multiple times if there are various feature collections
-->

<table class="featureInfo">
  <caption class="featureInfo">${type.name}</caption>
  <#if request.TIME??>
    <#assign request_time=request.TIME[0]?datetime?long>
  <#else>
    <b>TIME parameter is MISSING! Values will be wrong!</b><br/>
    Please add the TIME parameter to get the correct values<br/>
    Example: "...&TIME=2017-05-22T10:05:52.000Z"
    <#assign request_time=1>
  </#if>
  <tr>
  <th>fid</th>
<#list type.attributes as attribute>
  <#if !attribute.isGeometry>
    <th >${attribute.name}</th>
  </#if>
</#list>
  </tr>

<#assign odd = false>
<#list features as feature>
  <#if odd>
    <tr class="odd">
  <#else>
    <tr>
  </#if>
  <#assign odd = !odd>

  <td>${feature.fid}</td>    
  <#list feature.attributes as attribute>
    <#if !attribute.isGeometry>
      <#if attribute.name=="sst_dtime">
         <#if attribute.value?number gt 0>
           <#assign dtime=(attribute.value?number)*1000+request_time>
           <td>${(dtime)?number_to_datetime}</td>
         <#else>
            <td>Undefined</td>
         </#if>
      <#elseif attribute.name=="wind_speed_dtime_from_sst" || attribute.name=="sea_ice_fraction_dtime_from_sst">
         <#if attribute.value?number lt 9969000000000000000000000000000000000>
           <td>${(((attribute.value?number)*3600000)+dtime)?number_to_datetime}</td>
         <#else>
            <td>Undefined</td>
         </#if>
      <#else>
        <td>${attribute.value?string}</td>
      </#if>
    </#if>

  </#list>
  </tr>
</#list>
</table>
<br/>

