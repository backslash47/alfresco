<#escape x as jsonUtils.encodeJSONString(x)>
{
   "statusString": "${message(resultString)}",
   "statusCode": ${resultCode?string}
}
</#escape>