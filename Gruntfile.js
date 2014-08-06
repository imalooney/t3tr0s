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

grunt.registerTask('default', ['less']);

// end module.exports
};