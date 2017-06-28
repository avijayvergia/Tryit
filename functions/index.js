const functions = require('firebase-functions');
const Vision = require('@google-cloud/vision');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
const gcs = require('@google-cloud/storage')();

const visionClient = Vision({
    projectId: 'tryit-70fdc' 
});
const bucket = 'tryit-70fdc.appspot.com'; 

exports.guessIt = functions.database.ref('/feed/{feedId}')
.onWrite(event =>{

    const node=event.data.val();
    const fPath=node.filePath;
    const file= gcs.bucket(bucket).file(fPath);

    return visionClient.detectLabels(file)
        .then(data =>{
            return data[0];  
        }).then(labels =>{
            return admin.database()
            .ref('feed')
            .child(event.params.feedId)
            .set({
                downloadUrl: node.downloadUrl,
                filePath: node.filePath,
                uid: node.uid,
                title: labels[0]
            });
        }); 
}); 