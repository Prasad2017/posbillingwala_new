import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pos_billingwala_v2/app/app.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('Splash then licence login gate', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: PosBillingwalaApp()));

    expect(find.text('Billingwala'), findsWidgets);

    // Native logo hold + optional dynamic splash + bootstrap.
    await tester.pump(const Duration(milliseconds: 700));
    await tester.pump();
    await tester.pump(const Duration(seconds: 3));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));
    await tester.pump();

    expect(find.text('Welcome Back!'), findsOneWidget);
    expect(find.text('Licence key'), findsOneWidget);
    expect(find.text('Login'), findsOneWidget);
  });
}
