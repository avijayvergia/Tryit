const functions = require('firebase-functions');
const Vision = require('@google-cloud/vision');
const admin = require('firebase-admin');
const mkdirp = require('mkdirp-promise');
admin.initializeApp(functions.config().firebase);
const gcs = require('@google-cloud/storage')({
    keyFilename: 'tryit-70fdc-firebase-adminsdk-teyky-ba5bfe178b.json'
});
const spawn = require('child-process-promise').spawn;

const visionClient = Vision({
    projectId: 'tryit-70fdc'
});
const bucket = 'tryit-70fdc.appspot.com';

const LOCAL_TMP_FOLDER = '/tmp/';

// Max height and width of the thumbnail in pixels.
const THUMB_MAX_HEIGHT = 200;
const THUMB_MAX_WIDTH = 200;
// Thumbnail prefix added to file names.
const THUMB_PREFIX = 'thumb_';

exports.guessIt = functions.database.ref('/feed/{feedId}')
    .onWrite(event => {

        const node = event.data.val();
        const fPath = node.filePath;
        const file = gcs.bucket(bucket).file(fPath);

        return visionClient.detectLabels(file)
            .then(data => {
                return data[0];
            }).then(labels => {
                var str = '';
                labels.forEach(function (label) {
                    str += label + " ";
                });
                return admin.database()
                    .ref('feed')
                    .child(event.params.feedId)
                    .set({
                        downloadUrl: node.downloadUrl,
                        filePath: node.filePath,
                        uid: node.uid,
                        title: str
                    });
            });
    });

exports.generateThumbnail = functions.storage.object().onChange(event => {
    const fileBucket = event.data.bucket;
    const bucket = gcs.bucket(fileBucket);
    const filePath = event.data.name;
    const file = bucket.file(filePath);
    const filePathSplit = filePath.split('/');
    const fileName = filePathSplit.pop();
    const fileDir = filePathSplit.join('/') + (filePathSplit.length > 0 ? '/' : '');
    const thumbFilePath = `${fileDir}${THUMB_PREFIX}${fileName}`;
    const tempLocalDir = `${LOCAL_TMP_FOLDER}${fileDir}`;
    const tempLocalFile = `${tempLocalDir}${fileName}`;
    const tempLocalThumbFile = `${LOCAL_TMP_FOLDER}${thumbFilePath}`;
    const thumbFile = bucket.file(thumbFilePath);

    // Exit if the image is already a thumbnail.
    if (fileName.startsWith(THUMB_PREFIX)) {
        console.log('Already a Thumbnail.');
        return;
    }


    // Create the temp directory where the storage file will be downloaded.
    return mkdirp(tempLocalDir).then(() => {
        // Download file from bucket.
        return bucket.file(filePath).download({
            destination: tempLocalFile
        });
    }).then(() => {
        console.log('The file has been downloaded to', tempLocalFile);
        // Generate a thumbnail using ImageMagick.
        return spawn('convert', [tempLocalFile, '-thumbnail', `${THUMB_MAX_WIDTH}x${THUMB_MAX_HEIGHT}>`, tempLocalThumbFile]);
    }).then(() => {
        console.log('Thumbnail created at', tempLocalThumbFile);
        // Uploading the Thumbnail.
        return bucket.upload(tempLocalThumbFile, {
            destination: thumbFilePath
        })
    }).then(() => {
        console.log('Thumbnail uploaded to Storage at', thumbFilePath);
    }).then(() => {
        const config = {
            action: 'read',
            expires: '03-01-2500'
        };
        // Get the Signed URL for the thumbnail and original images
        return Promise.all([
            thumbFile.getSignedUrl(config),
            file.getSignedUrl(config),
        ]);
    }).then(results => {
        console.log('Got Signed URL');
        const thumbResult = results[0];
        const originalResult = results[1];
        const thumbFileUrl = thumbResult[0];
        const fileUrl = originalResult[0];
        // Add the URLs to the Database
        return admin.database()
            .ref().child('images').push({
                path: fileUrl,
                thumbnail: thumbFileUrl
            });
    }).catch(reason => {
        console.error(reason);
    });
})