<#assign content>
<div id="content">

<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>




<div id="outline" class="container">
<img src="img/chalkboard.png" alt ="board" width="1000" height="300">
<div class="center">
<div id= "program">
	<p>Autokorrect</p>
	<div id="change">
	<font size="2">
	<input  type="text" id="led" maxlength="2" size ="2" value="3"> Led
	<input  type="checkbox" class="largerCheckbox" id="prefix" value="Car"> Prefix
	<input  type="checkbox" class="largerCheckbox" id="whitespace" value="Bike"> Whitespace
	<input  type="checkbox" class="largerCheckbox" id="smart" value="Car"> Smart
	</font>
	</div>
</div>
<input type="text"  id ="autocorrect" name="autocorrect" value=""/><br>
<datalist id="auto">
    <option id ="suggest1" type="text" value=""/>
    <option id ="suggest2" type="text" value=""/>
    <option id ="suggest3" type="text" value=""/>
    <option id ="suggest4" type="text" value=""/>
    <option id ="suggest5" type="text" value=""/>
</datalist>
</div>
</div>


</div>
</#assign>
<#include "main.ftl">
