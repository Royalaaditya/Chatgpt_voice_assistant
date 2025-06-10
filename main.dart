import 'package:flutter/material.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:flutter_tts/flutter_tts.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter/services.dart';
import 'package:audioplayers/audioplayers.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ChatGPT Voice Assistant',
      theme: ThemeData.dark(),
      home: const AssistantPage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class AssistantPage extends StatefulWidget {
  const AssistantPage({super.key});
  @override
  State<AssistantPage> createState() => _AssistantPageState();
}

class _AssistantPageState extends State<AssistantPage> {
  final stt.SpeechToText _speech = stt.SpeechToText();
  final FlutterTts _tts = FlutterTts();
  final AudioPlayer _clonedPlayer = AudioPlayer();
  static const _channel = MethodChannel('chatgpt.assistant/channel');

  bool _isListening = false;
  bool _useClonedVoice = true;
  String _recognized = '';

  Future<void> _startListening() async {
    await Permission.microphone.request();
    bool available = await _speech.initialize(
        onStatus: (s) => debugPrint('Speech status: $s'),
        onError: (e) => debugPrint('Speech error: $e'));
    if (available) {
      setState(() => _isListening = true);
      _speech.listen(
        localeId: "hi_IN",
        onResult: (r) => setState(() => _recognized = r.recognizedWords),
      );
    }
  }

  Future<void> _stopAll() async {
    if (_isListening) {
      await _speech.stop();
    }
    await _tts.stop();
    await _clonedPlayer.stop();
    await _channel.invokeMethod('stopActions');
    setState(() => _isListening = false);
  }

  Future<void> _sendToChatGPT() async {
    if (_recognized.isEmpty) return;
    try {
      await _channel.invokeMethod('sendQuery', {'text': _recognized});
      String reply = await _channel.invokeMethod('readReply');
      if (_useClonedVoice) {
        await _channel.invokeMethod('speakCloned', {'text': reply});
      } else {
        await _tts.speak(reply);
      }
    } on PlatformException catch (e) {
      debugPrint('Channel error: \${e.message}');
    }
  }

  @override
  void dispose() {
    _speech.cancel();
    _tts.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
      floatingActionButton: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          FloatingActionButton(
            heroTag: 'mic',
            onPressed: _isListening ? null : _startListening,
            child: const Icon(Icons.mic),
          ),
          const SizedBox(width: 20),
          FloatingActionButton(
            heroTag: 'stop',
            backgroundColor: Colors.red,
            onPressed: _stopAll,
            child: const Icon(Icons.stop),
          ),
          const SizedBox(width: 20),
          FloatingActionButton(
            heroTag: 'send',
            onPressed: _sendToChatGPT,
            child: const Icon(Icons.send),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            SwitchListTile(
              title: const Text('क्लोन की गई आवाज़ प्रयोग करें'),
              value: _useClonedVoice,
              onChanged: (v) => setState(() => _useClonedVoice = v),
            ),
            const SizedBox(height: 20),
            Text(_recognized, style: const TextStyle(fontSize: 20)),
          ],
        ),
      ),
    );
  }
}
