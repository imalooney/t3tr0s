module.exports = function(grunt) {
'use strict';

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
    files: "public/css/*.less",
    tasks: ["less:watch"]
  }
  
});

// load tasks from npm
grunt.loadNpmTasks('grunt-contrib-less');
grunt.loadNpmTasks('grunt-contrib-watch');

// end module.exports
};