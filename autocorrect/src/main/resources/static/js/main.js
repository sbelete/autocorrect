var input = document.getElementById('autocorrect');
// Suggestion textboxes
var suggestions = [ document.getElementById('suggest1'),
		document.getElementById('suggest2'),
		document.getElementById('suggest3'),
		document.getElementById('suggest4'),
		document.getElementById('suggest5') ];

var suggs = formatSuggestions();

$(".selector").autocomplete({
	autoFocus : true
});

$("#autocorrect").keyup(function(event) {
	$("#autocorrect").css("background-color", "darkseagreen");

	if (event.keyCode < 37 || event.keyCode > 40) {
		getSuggestions();
		setAutocorrect();
	}
});

// Makes a post request to get suggestions for input
function getSuggestions() {

	var postParameters = {
		word : extractLast(input.value),
		prev : extractPrev(input.value),
		on : true
	};

	$.post("/auto", postParameters, parseSuggestions);
}

// Displays a list of suggestions
function parseSuggestions(suggestionsJSON) {

	// Get the suggestions list
	var suggestionsObject = JSON.parse(suggestionsJSON);
	var postSuggestions = $.map(suggestionsObject, function(e) {
		return e;
	});

	// Then, fill in the suggestions
	var i = 0;
	for (i; i < postSuggestions.length; i++) {
		suggestions[i].value = postSuggestions[i];
	}
	for (i; i < suggestions.length; i++) {
		suggestions[i].value = "";
	}

	setAutocorrect();
}

$("#autocorrect").autocomplete(
		{
			minLength : 0,
			source : function(request, response) {
				// delegate back to autocomplete, but
				// extract the last term
				response($.ui.autocomplete.filter(availableTags,
						extractLast(request.term)));
			},
			focus : function() {
				// prevent value inserted on focus
				return false;
			},
			select : function(event, ui) {
				var terms = split(this.value);
				// remove the current input
				terms.pop();
				// add the selected item
				terms.push(ui.item.value);
				// add placeholder to get the
				// comma-and-space at the end
				terms.push("");
				this.value = terms.join(" ");
				return false;
			}
		});

function setAutocorrect() {

	if (suggs != formatSuggestions()) {
		suggs = formatSuggestions();

		$("#autocorrect").autocomplete("option", "source", formatSuggestions());
		$("#autocorrect").autocomplete("search", "");
		$("#autocorrect").autocomplete("option", "autoFocus", true);
	}
}

function formatSuggestions() {
	var tester = [];

	for (var i = 0; i < suggestions.length; i++) {
		if (suggestions[i].value != "") {
			tester.push(String(suggestions[i].value));
		}
	}

	return tester;
}

function split(val) {
	return val.split(" ");
}

function extractLast(term) {
	return split(term).pop();
}

function extractPrev(term) {
	var temp = split(term);

	if (temp.length < 2) {
		return "";
	} else {
		temp.pop();
		return temp.pop();
	}

}

$("#autocorrect").bind(
		"keydown",
		function(event) {
			if (event.keyCode === $.ui.keyCode.TAB
					&& $(this).autocomplete("instance").menu.active) {
				event.preventDefault();
			}
		});

// //////////////////////////////////////////////////////////////////
// ///////////////////// js for modularity /////////////////////////
// ////////////////////////////////////////////////////////////////
$("#prefix").click(function(bool) {
	if ($(this).prop("checked") == true) {
		var postParameters = {
			change : "prefix",
			value : 1
		};
	} else {
		var postParameters = {
			change : "prefix",
			value : 0
		};
	}

	$.post("/update", postParameters, parseUpdate);
});

$("#whitespace").click(function(bool) {
	if ($(this).prop("checked") == true) {
		var postParameters = {
			change : "whitespace",
			value : 1
		};
	} else {
		var postParameters = {
			change : "whitespace",
			value : 0
		};
	}

	$.post("/update", postParameters, parseUpdate);
});

$("#smart").click(function(bool) {
	if ($(this).prop("checked") == true) {
		var postParameters = {
			change : "smart",
			value : 1
		};
	} else {
		var postParameters = {
			change : "smart",
			value : 0
		};
	}

	$.post("/update", postParameters, parseUpdate);
});

var led = document.getElementById('led');
var ledValue = led.value;
$("#led").css("background-color", "palegreen");

$("#led").keyup(function() {

	if (ledValue == led.value) {
		return;
	}
	ledValue = led.value;
	if (ledValue < 0) {
		$("#led").css("background-color", "#ff8080");
		return;
	}
	if (ledValue > 11) {
		$("#led").css("background-color", "#ff8080");
		return;
	}
	if (ledValue != parseInt(ledValue, 10)) {
		$("#led").css("background-color", "#ff8080");
		return;
	}

	$("#led").css("background-color", "palegreen");
	var postParameters = {
		change : "led",
		value : ledValue
	};

	$.post("/update", postParameters, parseUpdate);
});

var checks = [ $("#whitespace")[0], $("#prefix")[0], $("#smart")[0] ];
function parseUpdate(parseJSON) {
	// Get the suggestions list
	var suggestionsObject = JSON.parse(parseJSON);
	var postSuggestions = $.map(suggestionsObject, function(e) {
		return e;
	});
	
	for (var i = 0; i < 3; i++) {
		checks[i].checked = postSuggestions[i];
	}

	led.value = postSuggestions[3];
}

$.post("/update", {
	change : "start",
	value : 0
	}, parseUpdate);