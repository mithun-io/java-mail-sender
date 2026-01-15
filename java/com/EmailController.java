package com;

import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class EmailController {

	private final JavaMailSender mailSender;
	private final StringRedisTemplate redisTemplate;

	@GetMapping("/")
	public String load() {
		return "mail.html";
	}

	@GetMapping("/welcome")
	// @ResponseBody
	public String loadWelcome() {
		return "welcome.html";
	}

	@GetMapping("/otp")
	public String otp() {
		return "otp.html";
	}

	private int generateOtp() {
		return new Random().nextInt(100000, 1000000);
	}

	private int getFromRedis(String email) {
		String otp = redisTemplate.opsForValue().get(email);
		if (otp != null)
			return Integer.parseInt(otp);
		else
			return 0;
	}

	private void storeInRedis(String email, int otp) {
		redisTemplate.opsForValue().set(email, otp + "", Duration.ofMinutes(1));
	}

	private void sendEmail(int otp, UserDto dto) {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setFrom("admin@ecommerce.in", "e-commerce app");
			helper.setTo(dto.getEmail());
			helper.setSubject("Verification of Email thru OTP");
			String prefix = dto.getGender().equals("MALE") ? "Mr." : "Ms.";
			String content =
			        "<html><body>" +
			        "<h2>Account Verification</h2>" +
			        "<p>Hello " + prefix + " " + dto.getName() + ",</p>" +
			        "<p>Your OTP is:</p>" +
			        "<h1>" + otp + "</h1>" +
			        "<p>It is valid only for 1 minute.</p>" +
			        "</body></html>";


			helper.setText(content, true);

			mailSender.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@GetMapping("/send-otp")
	public String sendOtp(UserDto dto, RedirectAttributes attributes) {
		int otp = generateOtp();
		sendEmail(otp, dto);
		storeInRedis(dto.getEmail(), otp);
		attributes.addFlashAttribute("email", dto.getEmail());
		return "redirect:/otp";
	}

	@PostMapping("/otp")
	public String otp(@RequestParam String email, @RequestParam int otp, RedirectAttributes attributes) {
		int storedOtp = getFromRedis(email);
		if (storedOtp == 0) {
			attributes.addFlashAttribute("message", "otp expired, please try again");
			return "redirect:/";
		} else {
			if (storedOtp == otp) {
				attributes.addFlashAttribute("message", "successfully registered");
				return "redirect:/welcome";
			} else {
				attributes.addFlashAttribute("message", "otp mismatch");
				attributes.addFlashAttribute("email", email);
				return "redirect:/otp";
			}
		}
	}

}



