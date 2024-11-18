import Image from 'next/image';
import React from 'react';

import EmailIllustration from '@/public/images/email_illustration.svg';

export default function VerifyAccountPage() {
  return (
    <div className="flex flex-col min-h-screen">

      {/* Main Content */}
      <main className="flex-grow flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full text-center">
          <div className="bg-white p-8 rounded-lg shadow-lg">
            <div className="mb-6 flex justify-center">
              <EmailIllustration className="w-32 h-32" />
            </div>
            <h1 className="text-3xl font-semibold text-gray-800 mb-4">Verify Your Email</h1>
            <p className="text-gray-600 mb-6">
              A verification link has been sent to your email address. Please check your inbox and click the link to activate your account.
            </p>
            <p className="text-gray-500">
              Didn't receive the email?{' '}
              <a href="#" className="text-blue-600 hover:underline">
                Resend Verification Email
              </a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
