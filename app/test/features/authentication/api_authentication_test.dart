import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smart_parking_app/shared/api/api_client.dart';
import '../../helpers/fake_auth.dart';

class RecordingAdapter implements HttpClientAdapter {
  RecordingAdapter(this.statuses);
  final List<int> statuses;
  final headers = <String?>[];
  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream,
      Future<void>? cancelFuture) async {
    headers.add(options.headers['Authorization'] as String?);
    return ResponseBody.fromString('{}', statuses.removeAt(0), headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    });
  }
  @override
  void close({bool force = false}) {}
}

void main() {
  late FakeAuth auth;
  late Dio client;
  setUp(() {
    auth = FakeAuth(signedIn: true);
    client = createApiClient(auth, baseUrl: 'https://service.example/api/v1');
  });
  tearDown(() async {
    client.close();
    await auth.changes.close();
  });
  test('attaches current token and refreshes once after 401', () async {
    final adapter = RecordingAdapter([401, 200]);
    client.httpClientAdapter = adapter;
    expect((await client.get<dynamic>('/vehicles')).statusCode, 200);
    expect(adapter.headers, ['Bearer initial-token', 'Bearer refreshed-token']);
    expect(auth.refreshes, 1);
  });
  test('second 401 signs out without looping', () async {
    final adapter = RecordingAdapter([401, 401]);
    client.httpClientAdapter = adapter;
    await expectLater(client.get<dynamic>('/vehicles'), throwsA(isA<DioException>()));
    expect(adapter.headers.length, 2);
    expect(auth.refreshes, 1);
    expect(auth.signOuts, 1);
  });
  test('failed refresh signs out without retrying', () async {
    auth.refreshFails = true;
    final adapter = RecordingAdapter([401]);
    client.httpClientAdapter = adapter;
    await expectLater(client.get<dynamic>('/vehicles'), throwsA(isA<DioException>()));
    expect(adapter.headers.length, 1);
    expect(auth.signOuts, 1);
  });
  test('403 does not refresh or sign out', () async {
    client.httpClientAdapter = RecordingAdapter([403]);
    await expectLater(client.get<dynamic>('/vehicles'), throwsA(isA<DioException>()));
    expect(auth.refreshes, 0);
    expect(auth.signOuts, 0);
  });
  test('rejects other origins before sending any token', () async {
    final adapter = RecordingAdapter([200]);
    client.httpClientAdapter = adapter;
    await expectLater(client.get<dynamic>('https://other.example/api/v1'),
        throwsA(isA<DioException>()));
    expect(adapter.headers, isEmpty);
  });
}
