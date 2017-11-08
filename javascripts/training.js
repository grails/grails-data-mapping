var getJSON = function(url, callback) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.responseType = 'json';
    xhr.onload = function() {
        var status = xhr.status;
        if (status == 200) {
            callback(null, xhr.response);
        } else {
            callback(status);
        }
    };
    xhr.send();
};
function showElementsByClassName(className) {
    var elements = document.getElementsByClassName(className);
    for ( var i = 0; i < elements.length; i++ ) {
        var element = elements[i];
        element.style.display = "block";
    }
}

var ociTrainingTrack = 11;
getJSON('http://plugins.grails.org/api/training/'+ ociTrainingTrack, function(err, data) {
    var msg = '';
    if (err != null) {
        msg = 'Something went wrong while retrieving OCI training offerings';

    } else {
        if ( data.length == 0 ) {
            msg = '<p><b>Currenlty, we don\'t have any training offerings available</b></p>.';

        } else {
            msg += '<table>';
            msg += '<colgroup>';
            msg += '<col>';
            msg += '<col>';
            msg += '<col>';
            msg += '<col>';
            msg += '</colgroup>';
            msg += '<thead>';
            msg += '<tr><th>Course</th><th>Date(s)</th><th>Instructor(s)</th><th>Hour(s)</th></tr>';
            msg += '</thead>';
            msg += '<tbody>';
            for ( var i = 0; i < data.length; i++ ) {
                msg += '<tr><td><a href="'+ data[i].enrollmentLink + '">'+ data[i].course + '</a></td><td>'+ data[i].dates + '</td><td>'+ data[i].instructors + '</td><td>'+ data[i].hours + '</td></tr>';
            }
            msg += '</tbody>';
            msg += '</table>';
        }
    }
    var ociTraining = document.getElementById("ocitraining");
    ociTraining.innerHTML = msg;
    //showElementsByClassName('training')
});