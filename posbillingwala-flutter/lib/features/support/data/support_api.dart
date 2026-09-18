import 'package:dio/dio.dart';
import 'package:pos_billingwala_v2/core/constants/api_constants.dart';
import 'package:pos_billingwala_v2/core/network/api_client.dart';
import 'package:pos_billingwala_v2/core/network/api_response.dart';
import 'package:pos_billingwala_v2/features/support/data/support_dtos.dart';

class SupportApi {
  SupportApi(this.client);

  final ApiClient client;

  Future<List<SupportTicketDto>> getSupportTickets(
    String userId, {
    String status = '',
  }) async {
    final data = await supportApiGet(
      ApiEndpoints.getSupportTickets,
      query: {'userId': userId, 'status': status},
    );
    return mapJsonList(
      data[ApiResponseKeys.tickets],
      SupportTicketDto.fromJson,
    );
  }

  Future<StatusMessage> createSupportTicket({
    required String userId,
    required String category,
    required String subject,
    required String description,
    String? attachmentPath,
  }) async {
    final fields = <String, dynamic>{
      'userId': userId,
      'category': category,
      'subject': subject,
      'description': description,
    };
    final path = attachmentPath?.trim();
    if (path != null && path.isNotEmpty) {
      fields['attachment'] = await MultipartFile.fromFile(
        path,
        filename: path.split(RegExp(r'[\\/]')).last,
      );
    }
    final data = await supportApiPost(
      ApiEndpoints.createSupportTicket,
      fields: fields,
    );
    return StatusMessage.fromJson(data);
  }

  Future<SupportTicketDetailsDto> getTicketDetails(
    String userId,
    String ticketId,
  ) async {
    final data = await supportApiGet(
      ApiEndpoints.getSupportTicketDetails,
      query: {'userId': userId, 'ticketId': ticketId},
    );
    return SupportTicketDetailsDto.fromJson(data);
  }

  Future<StatusMessage> replyTicket({
    required String userId,
    required String ticketId,
    required String message,
  }) async {
    final data = await supportApiPost(
      ApiEndpoints.replySupportTicket,
      fields: {'userId': userId, 'ticketId': ticketId, 'message': message},
    );
    return StatusMessage.fromJson(data);
  }

  Future<Map<String, dynamic>> supportApiGet(
    String path, {
    Map<String, dynamic>? query,
  }) async {
    final response = await client.dio.get<dynamic>(
      path,
      queryParameters: query,
      options: Options(responseType: ResponseType.json),
    );
    return asJsonMap(response.data);
  }

  Future<Map<String, dynamic>> supportApiPost(
    String path, {
    required Map<String, dynamic> fields,
  }) async {
    final response = await client.dio.post<dynamic>(
      path,
      data: FormData.fromMap(fields),
      options: Options(responseType: ResponseType.json),
    );
    return asJsonMap(response.data);
  }
}
