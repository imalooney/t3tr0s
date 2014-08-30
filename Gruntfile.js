module.exports = function(grunt) {
'use strict';

//------------------------------------------------------------------------------
// UUCSS task
// TODO: this should become it's own module and published on npm
//------------------------------------------------------------------------------

function keys(o) {
  var a = [];
  for (var i in o) {
    if (o.hasOwnProperty(i) !== true) continue;
    a.push(i);
  }
  return a;
}

function arrToObj(arr) {
  var o = {};
  for (var i = 0; i < arr.length; i++) {
    o[ arr[i] ] = null;
  }
  return o;
}

function difference(arr1, arr2) {
  var o1 = arrToObj(arr1);
  var o2 = arrToObj(arr2);
  var delta = [];

  for (var i in o1) {
    if (o1.hasOwnProperty(i) !== true) continue;

    if (o2.hasOwnProperty(i) !== true) {
      delta.push(i)
    }
  }

  for (var i in o2) {
    if (o2.hasOwnProperty(i) !== true) continue;

    if (o1.hasOwnProperty(i) !== true) {
      delta.push(i)
    }
  }

  return delta.sort();
}

// UUCSS Class Names must contain at least one letter and one number
function hasNumbersAndLetters(str) {
  if (str.search(/\d/) === -1) {
    return false;
  }

  if (str.search(/[a-z]/) === -1) {
    return false;
  }

  return true;
}

// returns an array of unique UUCSS classes from a file
function extractUUCSSClasses(filename, pattern) {
  if (! pattern) {
    pattern = /([a-z0-9]+-){1,}([abcdef0-9]){5}/g;
  }

  var fileContents = grunt.file.read(filename);
  var classes = {};

  var matches = fileContents.match(pattern);

  if (matches) {
    for (var i = 0; i < matches.length; i++) {
      var c = matches[i];

      if (hasNumbersAndLetters(c) === true) {
        classes[c] = null;
      }
    }
  }

  return keys(classes);
}

function uucssCount() {
  var cssClasses = extractUUCSSClasses("public/css/t3tr0s.min.css");
  var jsClasses = extractUUCSSClasses("public/js/client.min.js");
  
  console.log(cssClasses.length + " class names found in css/t3tr0s.min.css");
  console.log(jsClasses.length + " class names found in js/client.min.js");

  console.log("Classes found in one file but not the other:");
  console.log( difference(jsClasses, cssClasses) );
}

//------------------------------------------------------------------------------
// Grunt Config
//------------------------------------------------------------------------------

grunt.initConfig({

  // LESS conversion
  less: {
    options: {
      yuicompress: true
    },

    watch: {
      files: {
        'public/css/t3tr0s.min.css': 'public/css/main.less'
      }
    }
  },

  watch: {
    options: {
      atBegin: true
    },
    files: "public/css/*.less",
    tasks: ["less:watch"]
  }

});

// load tasks from npm
grunt.loadNpmTasks('grunt-contrib-less');
grunt.loadNpmTasks('grunt-contrib-watch');

grunt.registerTask('uucss-count', uucssCount);
grunt.registerTask('default', ['less']);

// end module.exports
};