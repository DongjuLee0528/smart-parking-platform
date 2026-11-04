import 'package:dio/dio.dart';
import '../firebase/firebase_auth_boundary.dart';

final apiClient = createApiClient(FirebaseAuthentication());

Dio createApiClient(FirebaseAuthBoundary auth, {String? baseUrl}) {
  final dio = Dio(BaseOptions(
    baseUrl: baseUrl ?? const String.fromEnvironment(
      'API_BASE_URL', defaultValue: 'http://localhost:8080/api/v1',
    ),
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 15),
    followRedirects: false,
  ));
  final service = Uri.parse(dio.options.baseUrl);
  bool isService(RequestOptions options) =>
      options.uri.origin == service.origin &&
      (options.uri.path == service.path ||
          options.uri.path.startsWith('${service.path}/'));

  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      if (!isService(options)) {
        handler.reject(DioException(
          requestOptions: options, error: 'Requests must target the service API.',
        ));
        return;
      }
      try {
        final token = await auth.getIdToken();
        options.headers.remove('Authorization');
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      } catch (error) {
        handler.reject(DioException(requestOptions: options, error: error));
      }
    },
    onError: (error, handler) async {
      final options = error.requestOptions;
      if (error.response?.statusCode != 401 || !isService(options)) {
        handler.next(error);
        return;
      }
      if (options.extra['authRetried'] == true) {
        try { await auth.signOut(); } catch (_) { /* Preserve the API error. */ }
        handler.next(error);
        return;
      }
      try {
        final token = await auth.getIdToken(forceRefresh: true);
        if (token == null || token.isEmpty) throw StateError('Session expired');
      } catch (_) {
        try { await auth.signOut(); } catch (_) { /* Preserve the API error. */ }
        handler.next(error);
        return;
      }
      options.extra['authRetried'] = true;
      try {
        handler.resolve(await dio.fetch<dynamic>(options));
      } on DioException catch (retryError) {
        handler.next(retryError);
      }
    },
  ));
  return dio;
}
