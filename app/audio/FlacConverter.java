package audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javaFlacEncoder.FLACFileWriter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class FlacConverter {

	/**
	 * Convert the mp3 bytes to flac bytes.
	 */
	public byte[] getFlacBytes(byte[] data)
			throws UnsupportedAudioFileException, IOException {
		try (
		// create audioinputstream
		ByteArrayInputStream instream = new ByteArrayInputStream(data);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				// create audioinputstream from raw bytes
				AudioInputStream in = AudioSystem.getAudioInputStream(instream);
				// create wav audioinputstream
				AudioInputStream wavaos = wavStream(in);
				// create wav audioinputstream
				AudioInputStream conwavaos = convertSampleRate(16000, wavaos);) {
			// AudioSystem.write(conwavaos, AudioFileFormat.Type.WAVE, of);
			AudioSystem.write(conwavaos, FLACFileWriter.FLAC, os);
			// get bytes
			return os.toByteArray();
		}
	}

	/**
	 * Convert the input audio stream to wav.
	 */
	private AudioInputStream wavStream(AudioInputStream inStream) {
		AudioFormat baseFormat = inStream.getFormat();
		AudioFormat decodedFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
				16, baseFormat.getChannels(), baseFormat.getChannels() * 2,
				baseFormat.getSampleRate(), false);
		AudioInputStream outStream = AudioSystem.getAudioInputStream(
				decodedFormat, inStream);
		return outStream;
	}

	/**
	 * Convert the sample rate of a wav.
	 */
	private AudioInputStream convertSampleRate(float fSampleRate,
			AudioInputStream sourceStream) {
		AudioFormat sourceFormat = sourceStream.getFormat();
		AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(),
				fSampleRate, sourceFormat.getSampleSizeInBits(),
				sourceFormat.getChannels(), sourceFormat.getFrameSize(),
				fSampleRate, sourceFormat.isBigEndian());
		return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
	}

}
